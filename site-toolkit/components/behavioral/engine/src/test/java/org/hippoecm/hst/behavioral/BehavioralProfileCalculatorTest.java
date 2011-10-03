/*
 *  Copyright 2011 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.behavioral;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.TestCase;
import org.junit.Test;

public class BehavioralProfileCalculatorTest extends TestCase {
    
    @Test
    public void testPersonaScores() throws RepositoryException {
        Node configurationNode = session.getRootNode().getNode("behavioral:configuration");
        Configuration configuration = new Configuration(configurationNode);
        BehavioralProfileCalculator calculator = new BehavioralProfileCalculator(configuration);
        
        List<BehavioralData> behavioralDataList = new ArrayList<BehavioralData>();
        
        Map<String, Integer> termFreq = new HashMap<String, Integer>();
        String providerId = null;
        
        /*
         * rules that match document tags are configured to have a weight of 1
         * tagDataProvider is configured to have a weight of 1
         * 
         * the user visited a page that was marked with the term car
         * and two pages that were marked bicycle
         * resulting in the segment interests:cars to get an absolute score of 1 (freq) * 1 (rule weight) * 1 (provider weight)
         * and the segment interests:bikes to get an absolute score of 2 * 1 * 1
         */
        termFreq.put("car", new Integer(1));
        termFreq.put("bicycle", new Integer(2));
        providerId = "tagDataProvider";
        
        behavioralDataList.add(new BehavioralTestData(termFreq, providerId));
        
        /*
         * rules that match search terms are configured to have a weight of 2
         * searchDataProvider is configured to have a weight of 1
         * 
         * the user searched the site for the terms bicycles, wheelchair, and skateboard
         * resulting in the absolute score of the segment interests:bikes to increase by 1 * 2 * 1,
         * the segment age:old to get an absolute score of 1 * 2 * 1, and the segment age:young
         * to get an absolute score of 2 * 2 * 1
         */
        termFreq = new HashMap<String, Integer>();
        termFreq.put("bicycles", new Integer(1));
        termFreq.put("wheelchair", new Integer(1));
        termFreq.put("skateboard", new Integer(2));
        providerId = "searchDataProvider";
        
        behavioralDataList.add(new BehavioralTestData(termFreq, providerId));
        
        /*
         * somehow a non-existent term harvested by a non-existent provider
         * is (still) in our data set
         * this should have no effect on the calculation
         */
        termFreq = new HashMap<String, Integer>();
        termFreq.put("nonExistentTerm", new Integer(1));
        providerId = "nonExistentDataProvider";
        
        behavioralDataList.add(new BehavioralTestData(termFreq, providerId));
        
        /*
         * Within the dimension interests we have the two segments cars and bikes scoring an
         * absolute score of 1 and 4 respectively, resulting in the relative scores of these
         * segments within that dimension to be 20% and 80%.
         * 
         * Within the dimension age we have the two segments old and young scoring an
         * absolute score of 2 and 4 respectively, resulting in the relative scores of these
         * segments within that dimention to be 33% and 66 %
         * 
         * What this means for the persona scores is that the persona youngcyclist gets a score of 0.666 * 0.80 = 0.533, 
         * the persona oldcyclist 0.333 * 0.8 = 0.266 and the persona olddriver 0.33 * 0.2 = 0.066. 
         */
        BehavioralProfile profile = calculator.calculate(behavioralDataList);
        
        assertEquals(0.533, profile.getPersonaScore("youngcyclist").getScore() , 0.001);
        assertEquals(0.266, profile.getPersonaScore("oldcyclist").getScore() , 0.001);
        assertEquals(0.066, profile.getPersonaScore("olddriver").getScore() , 0.001);
    }

    private static class BehavioralTestData implements BehavioralData {

        private static final long serialVersionUID = 1L;

        private final Map<String, Integer> termFreq;
        private final String providerId;
        
        private BehavioralTestData(Map<String, Integer> termFreq, String providerId) {
            this.termFreq = termFreq;
            this.providerId = providerId;
        }
        
        @Override
        public Map<String, Integer> getTermFreq() {
            return this.termFreq;
        }

        @Override
        public String getProviderId() {
            return providerId;
        }
    }
}
