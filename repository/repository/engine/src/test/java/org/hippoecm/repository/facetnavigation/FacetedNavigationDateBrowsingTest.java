/*
 *  Copyright 2008 Hippo.
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

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FacetedNavigationDateBrowsingTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final static Calendar start = Calendar.getInstance();
    static {
        start.set(2009, 11, 23, 10, 46);
    }
    private final static Calendar onehourearlier = Calendar.getInstance();
    static {
        onehourearlier.set(2009, 11, 23, 9, 46);
    }
    private final static Calendar onedayearlier = Calendar.getInstance();
    static {
        onedayearlier.set(2009, 11, 22, 10, 46);
    }
    private final static Calendar threedayearlier = Calendar.getInstance();
    static {
        threedayearlier.set(2009, 11, 20, 10, 46);
    }
    private final static Calendar monthearlier = Calendar.getInstance();
    static {
        monthearlier.set(2009, 10, 23, 10, 46);
    }
    private final static Calendar monthandadayearlier = Calendar.getInstance();
    static {
        monthandadayearlier.set(2009, 10, 22, 10, 46);
    }
    private final static Calendar twomonthsearlier = Calendar.getInstance();
    static {
        twomonthsearlier.set(2009, 9, 23, 10, 46);
    }
    private final static Calendar yearearlier = Calendar.getInstance();
    static {
        yearearlier.set(2008, 11, 23, 10, 46);
    }

    private final static Calendar twoyearearlier = Calendar.getInstance();
    static {
        twoyearearlier.set(2007, 11, 23, 10, 46);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testDatesFacetNavigation() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure1(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getUUID());
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

    @Test
    public void testFacetSortingFacetNavigation() throws RepositoryException, IOException {
        
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDateStructure1(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getUUID());
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
                .getUUID());
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
                .getUUID());
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
    
    
    
    private void commonStart() throws RepositoryException {
        session.getRootNode().addNode("test");
        session.save();
    }

    private void createDateStructure1(Node test) throws RepositoryException {
        Node documents = test.addNode("documents", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        Node dateDocs = documents.addNode("datedocs", "nt:unstructured");
        documents.addMixin("mix:referenceable");

        addDateDoc(dateDocs, "datedoc1", start);
        addDateDoc(dateDocs, "datedoc2", onehourearlier);
        addDateDoc(dateDocs, "datedoc3", onedayearlier);
        addDateDoc(dateDocs, "datedoc4", threedayearlier);
        addDateDoc(dateDocs, "datedoc5", monthearlier);
        addDateDoc(dateDocs, "datedoc6", monthandadayearlier);
        addDateDoc(dateDocs, "datedoc7", twomonthsearlier);
        addDateDoc(dateDocs, "datedoc8", yearearlier);
        addDateDoc(dateDocs, "datedoc9", twoyearearlier);

        test.save();

    }

    private void addDateDoc(Node dateDocs, String name, Calendar cal) throws ItemExistsException,
            PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException,
            ConstraintViolationException, RepositoryException {
        Node dateDoc = dateDocs.addNode(name, "hippo:handle");
        dateDoc.addMixin("hippo:hardhandle");
        dateDoc = dateDoc.addNode(name, "hippo:testcardocument");
        dateDoc.addMixin("hippo:harddocument");
        dateDoc.setProperty("hippo:date", cal);

    }

}
