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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.Test;

public class FacetedNavigationGuidedBrowsingTest extends AbstractDateFacetNavigationTest {
    
    /*
     * This test is to make sure that you can configure that some facets are available only after some other facet has been chosen, and
     * some facets aren't visible anymore after some facet indicates that it removes another one.
     */
    @Test
    public void testGuidedFacetNavigation() throws RepositoryException, IOException {
        
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { 
                "hippo:date$year",  
                "hippo:date$month",
                "hippo:date$day",
                "hippo:brand"
                });
        
        /*
         * below, we configure, that the month facet will only be used *after* at least, the year facet has been chosen.
         * 
         * Thus initial available facets will be 'year' and 'brand'. After 'year' is chosen, 'month' becomes avaible. After month, day will be.
         * 
         * If after 'year' we browse through facet 'brand', still 'month' needs to be available
         */ 
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { 
                "year",
                "month${after:'year'}",
                "day${after:'month'}",
                "brand"
                });

        session.save();

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
      
        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode("2009"));

        // month should not be there yet, just as day
        assertFalse(facetNavigation.hasNode("month"));
        assertFalse(facetNavigation.hasNode("day"));
        
        // after year, the month is available, 
        assertTrue(facetNavigation.getNode("year").getNode("2009").hasNode("month"));
        // but not yet the day
        assertFalse(facetNavigation.getNode("year").getNode("2009").hasNode("day"));
        
        // after year and month, the day is available
        assertTrue(facetNavigation.getNode("year").getNode("2009").getNode("month").getNode("11").hasNode("day"));

        // also with brand in between, the month must be visible when year has been selected before:
        assertTrue(facetNavigation.getNode("year").getNode("2009").getNode("brand").getNode("peugeot").hasNode("month"));
        // also with brand in between, the day must be visible when year and month has been selected before:
        assertTrue(facetNavigation.getNode("year").getNode("2009").getNode("brand").getNode("peugeot").getNode("month").getNode("11").hasNode("day"));
        
        assertNotNull(facetNavigation.getNode("brand"));
        assertNotNull(facetNavigation.getNode("brand").getNode("peugeot"));
        
        assertTrue(facetNavigation.getNode("brand").getNode("peugeot").hasNode("year"));
        
        assertFalse(facetNavigation.getNode("brand").getNode("peugeot").hasNode("month"));
        assertTrue(facetNavigation.getNode("brand").getNode("peugeot").getNode("year").getNode("2009").hasNode("month"));
        

        /*
         * Now, we will configure, that after the 'year' is chosen, the facet won't be available again. This is by hiding itself from the list
         */
        
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { 
                "year${hide:'year'}",
                "month${after:'year', hide:'month'}",
                "day${after:'month'}",
                "brand"
                });

        session.save();

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
    
        assertTrue(facetNavigation.hasNode("year"));
        assertFalse(facetNavigation.hasNode("month"));
        assertTrue(facetNavigation.getNode("year").hasNode("2009"));
        assertTrue(facetNavigation.getNode("year").getNode("2009").hasNode("month"));
        assertTrue(facetNavigation.getNode("year").getNode("2009").hasNode("brand"));
        assertTrue(facetNavigation.getNode("year").getNode("2009").getNode("brand").hasNode("peugeot"));
        // year should not come back as it is hided
        assertFalse(facetNavigation.getNode("year").getNode("2009").hasNode("year"));
        assertTrue(facetNavigation.getNode("year").getNode("2009").getNode("month").hasNode("11"));
        assertFalse(facetNavigation.getNode("year").getNode("2009").getNode("month").getNode("11").hasNode("month"));
        
        /*
         * Now, we will configure, that after the 'year' is chosen, the facet 'brand' won't be available again. 
         * You can thus hide another facet when you have chosen the current one. 
         */
        
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { 
                "year${hide:'brand'}",
                "month${after:'year', hide:'year'}",
                "day${after:'month', hide:'month'}",
                "brand"
                });

        session.save();

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
   
        assertTrue(facetNavigation.hasNode("year"));
        assertFalse(facetNavigation.hasNode("month"));
        assertTrue(facetNavigation.getNode("year").hasNode("2009"));
        assertTrue(facetNavigation.getNode("year").getNode("2009").hasNode("month"));
        assertTrue(facetNavigation.getNode("year").getNode("2009").hasNode("year"));
        // brand shouldn't be available as the config for year is: year${hide:'brand'}
        assertFalse(facetNavigation.getNode("year").getNode("2009").hasNode("brand"));
        assertFalse(facetNavigation.getNode("year").getNode("2009").hasNode("day"));
        
        
        /*
         * after facet year, hide both brand and year
         */
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { 
                "year${hide:['brand', 'year']}",
                "month${after:'year', hide:'year'}",
                "day${after:'month', hide:'month'}",
                "brand${hide:'brand'}"
                });

        session.save();
        
        
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        
        assertTrue(facetNavigation.hasNode("year"));
        assertFalse(facetNavigation.hasNode("month"));
        assertTrue(facetNavigation.getNode("year").hasNode("2009"));
        assertTrue(facetNavigation.getNode("year").getNode("2009").hasNode("month"));
        // brand shouldn't be available as the config for year is: year${hide:'brand'}

        assertFalse(facetNavigation.getNode("year").getNode("2009").hasNode("year"));
        assertFalse(facetNavigation.getNode("year").getNode("2009").hasNode("brand"));
        assertFalse(facetNavigation.getNode("year").getNode("2009").hasNode("day"));
        
        assertFalse(facetNavigation.getNode("brand").getNode("peugeot").hasNode("brand"));
        
    }
    
}
