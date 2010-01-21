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

       Node navigation = testNode.addNode("facetnavigation");
       Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
       facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
               .getUUID());
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
       facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot", "contains(.,'french')" });

       session.save();

       navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
       
       FacetViewHelper.traverse(navigation);
       
       
   }
   

}
