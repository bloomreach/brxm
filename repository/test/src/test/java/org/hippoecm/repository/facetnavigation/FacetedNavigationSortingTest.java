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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.Test;

public class FacetedNavigationSortingTest extends AbstractDateFacetNavigationTest {
      
    /*
    * This test is to make sure sorting of the found facet values which are translated to nodenames are working as expected
    */
   @Test
   public void testFacetSortingNavigation() throws RepositoryException, IOException {
       
       commonStart();

       Node testNode = session.getRootNode().getNode("test");
       createDateStructure(testNode);

       Node navigation = testNode.addNode("facetnavigation");
       Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getIdentifier());
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { 
               "year${sortby:'facetvalue', sortorder:'ascending', limit:2}",
               "month${sortby:'facetvalue', sortorder:'descending'}"
               });

       session.save();

       Node nav1 = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
     
       // we have set a limit of 2, ordered on facetvalue (year number), ascending, hence these expectations
       assertNotNull(nav1.getNode("year"));
       assertEquals(9L, nav1.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT)
               .getLong());
       
       // We expect 3 nodes: 2 for the facet values (2007 and 2008), and one for the result set
       assertEquals(3L, nav1.getNode("year").getNodes().getSize());
       
       // first one should be 2007 because sortby:'facetvalue', sortorder:'ascending'
       assertTrue(nav1.getNode("year").getNodes().nextNode().getName().equals("2007"));
       
       
       
       /********* DIFFERENT ORDERING/ NO LIMIT NOW **************/
       
       navigation = testNode.getNode("facetnavigation");
       Node facetNavigation2 = navigation.addNode("hippo:navigation2", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation2.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getIdentifier());
       facetNavigation2.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year"});
       facetNavigation2.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { 
               "year${sortby:'facetvalue', sortorder:'descending'}"
               });

       session.save();

       Node nav2 = session.getRootNode().getNode("test/facetnavigation/hippo:navigation2");
       
       assertNotNull(nav2.getNode("year"));
       assertEquals(9L, nav2.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT)
               .getLong());
       
       // We expect all nodes (no limit): 3 for the facet values (2007/2008/2009), and one for the result set
       assertEquals(4L, nav2.getNode("year").getNodes().getSize());
       
       // first one should be 2009 because sortby:'facetvalue', sortorder:'descending'
       assertTrue(nav2.getNode("year").getNodes().nextNode().getName().equals("2009"));
       
       
      /********* DIFFERENT ORDERING/LIMIT NOW **************/
       
       navigation = testNode.getNode("facetnavigation");
       Node facetNavigation3 = navigation.addNode("hippo:navigation3", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation3.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getIdentifier());
       facetNavigation3.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year"});
       facetNavigation3.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { 
               "year${sortby:'count', sortorder:'ascending', limit:1}"
               });

       session.save();

       Node nav3 = session.getRootNode().getNode("test/facetnavigation/hippo:navigation3");
       
       assertNotNull(nav3.getNode("year"));
       assertEquals(9L, nav3.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT)
               .getLong());
       
       // We expect 2 nodes (no limit): 1 for the facet values (not sure which one, but at least one with a count of 1,
       // because we are sorting on count ascending), and one for the result set
       assertEquals(2L, nav3.getNode("year").getNodes().getSize());
       
       // first one should have a count of 1 because sortby:'count', sortorder:'ascending'
       assertEquals(1L, nav3.getNode("year").getNodes().nextNode().getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       
   }
}
