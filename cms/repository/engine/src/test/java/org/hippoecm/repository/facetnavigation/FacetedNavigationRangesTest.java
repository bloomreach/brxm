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
import org.hippoecm.repository.query.lucene.HippoDateTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FacetedNavigationRangesTest extends TestCase {
   
    private final static Calendar start = Calendar.getInstance();
    static {
       
    }
    private final static Calendar onehourbefore = Calendar.getInstance();
    static {
        onehourbefore.setTimeInMillis(start.getTimeInMillis());
        onehourbefore.add(Calendar.HOUR, -1);
    }
    private final static Calendar onedaybefore = Calendar.getInstance();
    static {
        onedaybefore.setTimeInMillis(start.getTimeInMillis());
        onedaybefore.add(Calendar.DAY_OF_YEAR, -1);
    }
    private final static Calendar threedaybefore = Calendar.getInstance();
    static {
        threedaybefore.setTimeInMillis(start.getTimeInMillis());
        threedaybefore.add(Calendar.DAY_OF_YEAR, -3);
    }
    private final static Calendar monthbefore = Calendar.getInstance();
    static {
        monthbefore.setTimeInMillis(start.getTimeInMillis());
        monthbefore.add(Calendar.MONTH, -1);
    }
    private final static Calendar monthandadaybefore = Calendar.getInstance();
    static {
        monthandadaybefore.setTimeInMillis(start.getTimeInMillis());
        monthandadaybefore.add(Calendar.MONTH, -1);
        monthandadaybefore.add(Calendar.DAY_OF_YEAR, -1);
    }
    private final static Calendar twomonthsbefore = Calendar.getInstance();
    static {
        twomonthsbefore.setTimeInMillis(start.getTimeInMillis());
        twomonthsbefore.add(Calendar.MONTH, -2);
    }
    private final static Calendar yearbefore = Calendar.getInstance();
    static {
        yearbefore.setTimeInMillis(start.getTimeInMillis());
        yearbefore.add(Calendar.YEAR, -1);
    }
    
    private final static Calendar twoyearbefore = Calendar.getInstance();
    static {
        twoyearbefore.setTimeInMillis(start.getTimeInMillis());
        twoyearbefore.add(Calendar.YEAR, -2);
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
        /*
         * This test should not run just around the swapping of day (so, at 23:59 hours), as in this case, the expectations below with 
         * runtime calendar based ranges might return different values then expected in the unit test: this is not wrong, but we cannot anticipate 
         * this in the tests. Hence, when there are less then 5 minutes left in the current day, we skip this test 
         */
        Calendar noon = Calendar.getInstance();
        // get next day
        noon.add(Calendar.DAY_OF_YEAR, 1);
        //
        long timeMillisSecAtNoon = HippoDateTools.round(noon.getTimeInMillis(), HippoDateTools.Resolution.DAY);
        
        noon.setTimeInMillis(timeMillisSecAtNoon);
      
        // 300.000 millesec = 5 min
        if( (timeMillisSecAtNoon - start.getTimeInMillis()) < 300 * 1000) {
            // we do not run this unit test, as around the swapping of a day
            assertTrue(true);
            return;
        }
        
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure1(testNode);
        
        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", HippoNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getUUID());
        facetNavigation.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {"hippo:date$[{name:'yesterday', resolution:'day', begin:-1, end:0}, {name:'today', resolution:'day', begin:0, end:1}]"});
        facetNavigation.setProperty(HippoNodeType.HIPPO_FACETNODENAMES, new String[] {"range"});
        
        session.save();

        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
  
        assertNotNull(node.getNode("range"));
        assertNotNull(node.getNode("range/today"));
        assertNotNull(node.getNode("range/today/hippo:resultset"));
        assertEquals(2L, node.getNode("range/today").getProperty("hippo:count").getLong());
        
        assertNotNull(node.getNode("range/yesterday"));
        assertNotNull(node.getNode("range/yesterday/hippo:resultset"));
        assertEquals(1L, node.getNode("range/yesterday").getProperty("hippo:count").getLong());
        
        
    }
    
    
    private void commonStart() throws RepositoryException{
        session.getRootNode().addNode("test");
        session.save();
    }
    
    private void createDateStructure1(Node test) throws RepositoryException {
        Node documents = test.addNode("documents","nt:unstructured");
        documents.addMixin("mix:referenceable");
        Node dateDocs = documents.addNode("datedocs","nt:unstructured");
        documents.addMixin("mix:referenceable");

        addDateDoc(dateDocs, "start", start);
        addDateDoc(dateDocs, "onehourbefore", onehourbefore);
        addDateDoc(dateDocs, "onedaybefore", onedaybefore);
        addDateDoc(dateDocs, "threedaybefore", threedaybefore);
        addDateDoc(dateDocs, "monthbefore", monthbefore);
        addDateDoc(dateDocs, "monthandadaybefore", monthandadaybefore);
        addDateDoc(dateDocs, "twomonthsbefore", twomonthsbefore);
        addDateDoc(dateDocs, "yearbefore", yearbefore);
        addDateDoc(dateDocs, "twoyearbefore", twoyearbefore);
        
        test.save();
        
    }

    private void addDateDoc(Node dateDocs, String name, Calendar cal) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        Node dateDoc  = dateDocs.addNode(name,"hippo:handle");
        dateDoc.addMixin("hippo:hardhandle");
        dateDoc = dateDoc.addNode(name,"hippo:testcardocument");
        dateDoc.addMixin("hippo:harddocument");
        dateDoc.setProperty("hippo:date", cal);
        
    }
    
}
