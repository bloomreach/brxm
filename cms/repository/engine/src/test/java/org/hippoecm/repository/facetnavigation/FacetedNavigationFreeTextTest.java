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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.Test;
public class FacetedNavigationFreeTextTest extends AbstractDateFacetNavigationTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
      
    
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

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{jumps}]");
        
        // We only have three cars that contain jumps!!
        
        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)));

        // NOTE WE HAVE 3 now instead of 7
        assertEquals(3L, facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());
        
         
    }
    
    @Test
    public void testSimpleFreeTextDirectAfterMirrorSearch() throws RepositoryException, IOException {
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
        
        // direct access with search, so no filter:
        
        
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{green}]");
        // We have three green cars
        Long count = facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong();
        assertEquals(3L, (long)count);
        
        session.refresh(false);
        
        facetselect = testNode.getNode("filtered");
      
        // access through facetselect with search
        Node facetFreeSearchNavigationNode = facetselect.getNode("hippo:navigation[{green}]");
       
        count = facetFreeSearchNavigationNode.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong();
        
        // We have only one green peugeot
        assertEquals(1L, (long)count);
        
    }

    
    /**
     * Same as test {@link #testSimpleFreeTextDirectAfterMirrorSearch()} only this time we link the filter
     * to a higher ancestor of the faceted navigation node. This means, the free text search & filters need to be passed on 
     * by multiple 'mirrored/filtered' node ids. which behaves a little different then in the case of
     * {@link #testSimpleFreeTextDirectAfterMirrorSearch()}
     * @throws RepositoryException
     * @throws IOException
     */
    @Test
    public void testSimpleFreeTextInDirectMirrorSearch() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        // first create a test2 node now:
        Node testNode2 = testNode.addNode("test2");
        testNode2.addMixin("mix:referenceable");
        Node facetNavigation = testNode2.addNode("facetnavigation");
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
        String docbase = testNode.getNode("test2").getUUID();
        Node facetselect = testNode.addNode("filtered", HippoNodeType.NT_FACETSELECT);
        facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, docbase);
        facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] { "select" });
        facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:brand" });
        facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "peugeot" });
       
        session.save();
        session.refresh(false);
        
        facetselect = testNode.getNode("filtered");
        
        // we know there are 5 peugeots, so only these should make it through the filter
        
        assertTrue(" We expect 5 cars because of the inherited filter",facetselect.getNode("facetnavigation/hippo:navigation").getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong() == 5L);
        
        // now, also add FreeTextSearch: we know there are 3 peugeots which contains 'jumps'
        
        session.refresh(false);
        
        // direct access with search, so no filter:
        
        facetNavigation = session.getRootNode().getNode("test/test2/facetnavigation/hippo:navigation[{green}]");
        // We have three green cars
        Long count = facetNavigation.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong();
        assertEquals(3L, (long)count);
        
        session.refresh(false);
        
        facetselect = testNode.getNode("filtered");
        
        // access through facetselect with search. We have only ONE green peugeot
        Node facetFreeSearchNavigationNode = facetselect.getNode("facetnavigation/hippo:navigation[{green}]");
       
        count = facetFreeSearchNavigationNode.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong();
        assertEquals("There should be 1 green peugeot",1L, (long)count);
      
    }

}
