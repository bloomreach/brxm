/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.facetnavigation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.Test;

public class FacetedNavigationDateBrowsingTest extends AbstractDateFacetNavigationTest {
    
    @Test
    public void testDatesFacetNavigation() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year", "hippo:date$month", "hippo:date$day" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "year", "month", "day" });

        session.save();

        int currentYear = start.get(Calendar.YEAR);
        int yearAgo = yearearlier.get(Calendar.YEAR);
        int twoYearsAgo = twoyearearlier.get(Calendar.YEAR);
        int currentMonth = start.get(Calendar.MONTH);
        int currentDay = start.get(Calendar.DAY_OF_MONTH);

        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
           
        assertNotNull(node.getNode("year"));
        assertNotNull(node.getNode("year").getNode(String.valueOf(currentYear)));

        assertTrue(node.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 7L);
        assertTrue(node.getNode("year").getNode(String.valueOf(yearAgo)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 1L);
        assertTrue(node.getNode("year").getNode(String.valueOf(twoYearsAgo)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 1L);

        assertNotNull(node.getNode("month"));
        assertNotNull(node.getNode("month").getNode(String.valueOf(currentMonth)));

        assertNotNull(node.getNode("day"));
        assertNotNull(node.getNode("day").getNode(String.valueOf(currentDay)));
    }

}
