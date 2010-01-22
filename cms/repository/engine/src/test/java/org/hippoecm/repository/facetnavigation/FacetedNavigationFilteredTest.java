/*
 *  Copyright 2010 Hippo.
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.Test;

public class FacetedNavigationFilteredTest extends AbstractDateFacetNavigationTest {
      
    /*
    * This test is to make sure filtering of the faceted navigation works as expected
    */
   @Test
   public void testFacetFilteredNavigation() throws RepositoryException, IOException {
       
       commonStart();

       Node testNode = session.getRootNode().getNode("test");
       createDateStructure1(testNode);

       Node facetNavigation = testNode.addNode("facetnavigation");
       facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getUUID());
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,'jumps')" });

       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // there are three peugeot cars which contain 'jumps' in a child node
       assertEquals(3L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       //assertEquals(3L, navigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       // change the filter:
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,'quick')" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       // we only have one peugeot which contains 'quick' in a child node
       assertEquals(1L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       //assertEquals(1L, navigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       
      // change the filter:
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,'quick brown')" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       // we have one car with 'brown' and one car with 'quick brown' : default operator of space is OR
       assertEquals(2L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       //assertEquals(2L, navigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
       
      // change the filter:
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,'quick AND brown')" });
       session.save();
       facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       // we have one car with 'brown' and one car with 'quick brown' : since operator is AND, we expect 1 car
       assertEquals(1L, facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       //assertEquals(1L, navigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
   }
   

}
