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

import org.hippoecm.repository.util.Utilities;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.Test;
public class HREPTWO4457IssueFacetedNavigationFreeTextTest extends AbstractDateFacetNavigationTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: FacetedNavigationFilteredTest.java 21571 2010-02-08 11:34:42Z bvanhalderen $";
      
    
    @Test
    public void testSimpleFreeTextSearch() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getUUID());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
      
        session.save();
        
        int currentYear = start.get(Calendar.YEAR);

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)));

        assertTrue(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 7L);
        
        session.refresh(false);

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[[jumps]]");
        //facetNavigation = session.getRootNode().getNode("test/facetnavigation");
        //facetNavigation = facetNavigation.getNode("hippo:navigation[[jumps]]/year");
        //facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        
        // We only have three cars that contain jumps!!
        
        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)));

        // NOTE WE HAVE 3 now instead of 7
        assertEquals(3L, facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());
        
         
    }
    
    @Test
    public void testSimpleFreeTextAfterMirrorSearch() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation.addMixin("mix:referenceable");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getUUID());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
      
        /*
         * let's create a facetselect with filter, that points to the faceted navigation.
         * The criteria is, that we only want to see brand = peugeot. This needs to be 
         * reflected in the count numbers & resultset and facetvalue in the faceted navigation as well
         */

        testNode = session.getRootNode().getNode("test");
        String docbase = testNode.getNode("facetnavigation").getUUID();
        Node facetselect = testNode.addNode("filtered", HippoNodeType.NT_FACETSELECT);
        facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, docbase);
        facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] { "select" });
        facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:brand" });
        facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "peugeot" });
       
        session.save();
        session.refresh(false);
        
        facetselect = testNode.getNode("filtered");
        
        // we know there are 5 peugeots, so only these should make it through the filter
        
        assertTrue(" We expect 5 cars because of the inherited filter",facetselect.getNode("hippo:navigation").getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong() == 5L);
        
        // now, also add FreeTextSearch: we know there are 3 peugeots which contains 'jumps'
        
        session.refresh(false);
        
        facetselect = testNode.getNode("filtered");
        
        // direct access with search
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{jumps}]");
      
        
        //TODO  HREPTWO-4457 : SHOULD FIND THE NODE hippo:navigation[{jumps}] but returns a PathNotFoundException
        
        // access through facetselect with search
        Node facetFreeSearchNavigationNode = facetselect.getNode("hippo:navigation[{jumps}]");
       
        assertEquals(3L, facetselect.getNode("hippo:navigation").getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
    }

}
