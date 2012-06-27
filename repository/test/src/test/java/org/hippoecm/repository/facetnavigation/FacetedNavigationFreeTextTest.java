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
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.Test;
public class FacetedNavigationFreeTextTest extends AbstractDateFacetNavigationTest {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
      
    
    @Test
    public void testSimpleFreeTextSearch() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
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
    public void testSimpleXPathSearches() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
      
        session.save();
        
        int currentYear = start.get(Calendar.YEAR);

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        
        // assert there is a correct count property:
        assertTrue(facetNavigation.hasProperty("hippo:count"));
        assertEquals(9L, facetNavigation.getProperty("hippo:count").getLong());
        assertTrue(facetNavigation.hasNode("hippo:resultset"));
        assertTrue(facetNavigation.getNode("hippo:resultset").hasProperty("hippo:count"));
        assertEquals(9L , facetNavigation.getNode("hippo:resultset").getProperty("hippo:count").getLong());
        assertEquals(9L , facetNavigation.getNode("hippo:resultset").getNodes().getSize());
        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)));

        assertTrue(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 7L);
        
        session.refresh(false);
        
        String xpath = "xpath(//*[jcr:contains(.,'jumps')])";
        
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{"+xpath+"}]");
        // WE HAVE 3 cars that contains jumps.
        assertEquals(3L, facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());
        
        
        session.refresh(false);
        
        // search in only the content
        xpath = "xpath(//*[jcr:contains(contents/@content,'jumps')])";
        
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{"+xpath+"}]");
        
        // WE HAVE 3 cars that contains jumps.
        assertTrue(facetNavigation.hasProperty("hippo:count"));
        assertEquals(3L, facetNavigation.getProperty("hippo:count").getLong());
        assertTrue(facetNavigation.hasNode("hippo:resultset"));
        assertTrue(facetNavigation.getNode("hippo:resultset").hasProperty("hippo:count"));
        assertEquals(3L , facetNavigation.getNode("hippo:resultset").getProperty("hippo:count").getLong());
        assertEquals(3L , facetNavigation.getNode("hippo:resultset").getNodes().getSize());
        assertEquals(3L, facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());
        
        session.refresh(false);
        xpath = "xpath(//*[@hippo:brand = 'peugeot'])";
        
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{"+xpath+"}]");
        // WE HAVE 5 peugeots
        assertEquals(5L, facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());
        
        session.refresh(false);
        xpath = "xpath(//*[@hippo:brand = 'peugeot' and @hippo:color='red'])";
        
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{"+xpath+"}]");
        // WE HAVE 3 red peugeots
        assertEquals(3L, facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());
        
        session.refresh(false);
        xpath = "xpath(//*[@hippo:brand = 'peugeot' and @hippo:color='red' and jcr:contains(.,'jumps')])";
        
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{"+xpath+"}]");
        // WE HAVE 1 red peugeot that contains 'jumps'
        assertEquals(1L, facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());
        
    }
    
    @Test
    public void testMultipleScopesAndFreeText() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        String multiScopeDocBases = session.getRootNode().getNode("test/documents/cardocs/cardoc1").getIdentifier() + "," +
                                    session.getRootNode().getNode("test/documents/cardocs/cardoc6").getIdentifier();;
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, multiScopeDocBases);
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
      
        session.save();
        
        int currentYear = start.get(Calendar.YEAR);

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)));

        // we have as multiple scopes two cars only
        assertTrue(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 2L);
        
        // combine a free text search now as well:
        session.refresh(false);

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{jumps}]");
        
        // only cardoc1 contains 'jumps'. cardoc6 does not, so we expect 1 result 
        assertTrue(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 1L);
    }
    
    @Test
    public void testMultipleScopesAndXPath() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        String multiScopeDocBases = session.getRootNode().getNode("test/documents/cardocs/cardoc1").getIdentifier() + "," +
                                    session.getRootNode().getNode("test/documents/cardocs/cardoc6").getIdentifier();;
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, multiScopeDocBases);
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
      
        session.save();
        
        int currentYear = start.get(Calendar.YEAR);

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)));

        // we have as multiple scopes two cars only
        assertTrue(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 2L);
        
        // combine a free text search now as well:
        session.refresh(false);

        String xpath = "xpath(//*[@hippo:brand = 'peugeot'])";
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{"+xpath+"}]");
        
        // only cardoc1 is of brand 'peugeot'. cardoc6 does not, so we expect 1 result 
        assertTrue(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 1L);
    }
    
    @Test
    public void testMultipleScopesAndFiltersAndXPath() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        String multiScopeDocBases = session.getRootNode().getNode("test/documents/cardocs/cardoc1").getIdentifier() + "," +
                                    session.getRootNode().getNode("test/documents/cardocs/cardoc6").getIdentifier();;
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, multiScopeDocBases);
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FILTERS, new String[] {"hippo:brand=peugeot"});
        
        session.save();
        
        int currentYear = start.get(Calendar.YEAR);

        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)));

        // we have as multiple scopes two cars only but due to filter only one car, peugeot we should find
        assertTrue(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 1L);
        
        // combine a free text search now as well:
        session.refresh(false);

        String xpath = "xpath(//*[@hippo:brand = 'peugeot' and jcr:contains(.,'jumps')])";
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{"+xpath+"}]");
        
        // only cardoc1 is of brand 'peugeot'. cardoc6 does not, so we expect 1 result 
        assertTrue(facetNavigation.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 1L);
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
                .getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
      
        /*
         * let's create a facetselect with filter, that points to the faceted navigation.
         * The criteria is, that we only want to see brand = peugeot. This needs to be 
         * reflected in the count numbers & resultset and facetvalue in the faceted navigation as well
         */

        testNode = session.getRootNode().getNode("test");
        String docbase = testNode.getNode("facetnavigation").getIdentifier();
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
       
        // We have only one green peugeot
        // assert there is a correct count property:
        assertTrue(facetFreeSearchNavigationNode.hasProperty("hippo:count"));
        assertEquals(1L, facetFreeSearchNavigationNode.getProperty("hippo:count").getLong());
        assertTrue(facetFreeSearchNavigationNode.hasNode("hippo:resultset"));
        assertTrue(facetFreeSearchNavigationNode.getNode("hippo:resultset").hasProperty("hippo:count"));
        assertEquals(1L , facetFreeSearchNavigationNode.getNode("hippo:resultset").getProperty("hippo:count").getLong());
        assertEquals(1L , facetFreeSearchNavigationNode.getNode("hippo:resultset").getNodes().getSize());
       
        // We have only one green peugeot
        assertEquals(1L, facetFreeSearchNavigationNode.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
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
                .getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
      
        /*
         * let's create a facetselect with filter, that points to the faceted navigation.
         * The criteria is, that we only want to see brand = peugeot. This needs to be 
         * reflected in the count numbers & resultset and facetvalue in the faceted navigation as well
         */

        testNode = session.getRootNode().getNode("test");
        String docbase = testNode.getNode("test2").getIdentifier();
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
       
        assertTrue(facetFreeSearchNavigationNode.hasProperty("hippo:count"));
        assertEquals(1L, facetFreeSearchNavigationNode.getProperty("hippo:count").getLong());
        assertTrue(facetFreeSearchNavigationNode.hasNode("hippo:resultset"));
        assertTrue(facetFreeSearchNavigationNode.getNode("hippo:resultset").hasProperty("hippo:count"));
        assertEquals(1L , facetFreeSearchNavigationNode.getNode("hippo:resultset").getProperty("hippo:count").getLong());
        assertEquals(1L , facetFreeSearchNavigationNode.getNode("hippo:resultset").getNodes().getSize());
        assertEquals("There should be 1 green peugeot",1L, facetFreeSearchNavigationNode.getNode("year").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
      
    }

    
    @Test
    public void testXPathAndOrderBySearches() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node facetNavigation = testNode.addNode("facetnavigation");
        facetNavigation = facetNavigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date$year",  "hippo:date$month"});
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"year","month"});
      
        // we set the default ordering on the faceted navigation to 'hippo:color' (default is ascending)
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTBY, new String[] { "hippo:color" });
        
        session.save();
   
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
  
        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode("hippo:resultset"));

        assertTrue(facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 9L);
        
        // we sorted on hippo:color in the faceted navigation configuration, let's confirm this:
        
        assertTrue(isCorrectOrder(facetNavigation.getNode("hippo:resultset").getNodes(), "hippo:color", true));
             
        // check resultset below year facet
        assertTrue(isCorrectOrder(facetNavigation.getNode("year").getNode("hippo:resultset").getNodes(), "hippo:color", true));
        
        // now we add new ordering (on hippo:brand) through xpath injected
        session.refresh(false);
        
        // default order is ascending
        String xpath = "xpath(//* order by @hippo:brand)";
        
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{"+xpath+"}]");
        // we still have 9 cars
        
        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode("hippo:resultset"));

        assertTrue(facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 9L);
      
        // we sorted on hippo:brand in the faceted navigation configuration, let's confirm this:
        assertTrue(isCorrectOrder(facetNavigation.getNode("hippo:resultset").getNodes(), "hippo:brand", true));
        assertTrue(isCorrectOrder(facetNavigation.getNode("year").getNode("hippo:resultset").getNodes(), "hippo:brand", true));
        
       // now we add new ordering (on hippo:brand) but now REVERSE the ordering
        session.refresh(false);
        
        xpath = "xpath(//* order by @hippo:brand descending)";
        
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation[{"+xpath+"}]");
        // we still have 9 cars
        
        assertNotNull(facetNavigation.getNode("year"));
        assertNotNull(facetNavigation.getNode("year").getNode("hippo:resultset"));

        assertTrue(facetNavigation.getNode("year").getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong() == 9L);
      
        // we sorted on hippo:brand in the faceted navigation configuration, let's confirm this:
        assertTrue(isCorrectOrder(facetNavigation.getNode("hippo:resultset").getNodes(), "hippo:brand", false));
        assertTrue(isCorrectOrder(facetNavigation.getNode("year").getNode("hippo:resultset").getNodes(), "hippo:brand", false));
        
    }
    
    private boolean isCorrectOrder(NodeIterator nodes, String property, boolean ascending) throws RepositoryException{
        {
            String prevValue = null;
            while(nodes.hasNext()) {
                String nextValue = nodes.nextNode().getProperty(property).getString();
                if(prevValue != null && nextValue.compareTo(prevValue) < 0 && ascending) {
                    // not an ascending order
                    return false;
                }
                if(prevValue != null &&  nextValue.compareTo(prevValue) > 0 && !ascending) {
                    // not an descending order
                    return false;
                }
                prevValue = nextValue;
            }
        }
        return true;
    }
    
}
