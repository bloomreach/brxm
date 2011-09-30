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
package org.onehippo.hst.behavioral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        Node configurationNode = session.getRootNode().getNode("behavioral-configuration");
        Configuration configuration = new Configuration(configurationNode);
        BehavioralProfileCalculator calculator = new BehavioralProfileCalculator(configuration);
        
        List<BehavioralData> behavioralDataList = new ArrayList<BehavioralData>();
        
        Map<String, Integer> termFreq = new HashMap<String, Integer>();
        String providerId = null;
        
        /*
         * rules that match document tags are configured to have a weight of 1
         * tagDataProvider is configured to have a weight of 1
         * 
         * the user visited a page that was marked with the term renault,
         * two pages that were marked giant, and one that was marked about-us
         * resulting in the segment interests:cars to get an absolute score of 1 (freq) * 1 (rule weight) * 1 (provider weight),
         * the segment interests:bikes to get an absolute score of 2 * 1 * 1,
         * and the segment contact:interestedvisitor to get an absolute score of 1 * 1 * 1
         */
        termFreq.put("renault", new Integer(1));
        termFreq.put("giant", new Integer(2));
        termFreq.put("about-us", new Integer(1));
        providerId = "tagDataProvider";
        
        behavioralDataList.add(new BehavioralTestData(termFreq, providerId));
        
        /*
         * rules that match search terms are configured to have a weight of 2
         * searchDataProvider is configured to have a weight of 1
         * 
         * the user searched the site for the terms bicycle, wheelchair, and skateboard
         * resulting in the absolute score of the segment interests:bikes to increase by 1 * 2 * 1,
         * the segment age:old to get an absolute score of 1 * 2 * 1, and the segment age:young
         * to get an absolute score of 1 * 2 * 1
         */
        termFreq = new HashMap<String, Integer>();
        termFreq.put("bicycle", new Integer(1));
        termFreq.put("wheelchair", new Integer(1));
        termFreq.put("skateboard", new Integer(1));
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
         * Within the dimension age we have the two segments both scoring an absolute score
         * of 1, resulting in both to have a relative score of 50% in that dimension
         * 
         * Within the dimension contact (segments newvisitor and interestedvisitor) we have
         * a score only for interested visitor (absolute score of 1 because the about-us page
         * was viewed) but no score for the other segment which results in the segment interestedvisitor
         * to have a relative score of 100% in that dimension
         * 
         * What this means for the persona scores is that the persona driver gets 0.2 points, the persona
         * cyclist 0.8 points and the persona oldinterestedvisitor 0.5 * 1.0 points. normalizing these
         * absolute scores given the totalscore of 0.2 + 0.8 + 0.5 = 1.5 we get that the persona driver scores
         * 0.2 / 1.5 = 1.33333etc, the persona cyclist scores 0.8 / 1.5 = 0.533333etc, 
         * and the persona oldinterestedvisitor scores 0.5 / 1.5 = 0.3333333etc
         */
        BehavioralProfile profile = calculator.calculate(behavioralDataList);
        
        assertNotNull(profile.getPrincipalPersonaId());
        assertEquals(profile.getPrincipalPersonaId(), "cyclist");
        assertEquals(profile.getPersonaScores().size(), 3);
        assertEquals(profile.getPrincipalPersonaScore().getScore().doubleValue(), 0.8 / 1.5, 0.000001);
        
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
