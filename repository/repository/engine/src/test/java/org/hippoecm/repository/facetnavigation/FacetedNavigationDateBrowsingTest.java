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
    private final static Calendar onehourbefore = Calendar.getInstance();
    static {
        onehourbefore.set(2009, 11, 23, 9, 46);
    }
    private final static Calendar onedaybefore = Calendar.getInstance();
    static {
        onedaybefore.set(2009, 11, 22, 10, 46);
    }
    private final static Calendar threedaybefore = Calendar.getInstance();
    static {
        threedaybefore.set(2009, 11, 20, 10, 46);
    }
    private final static Calendar monthbefore = Calendar.getInstance();
    static {
        monthbefore.set(2009, 10, 23, 10, 46);
    }
    private final static Calendar monthandadaybefore = Calendar.getInstance();
    static {
        monthandadaybefore.set(2009, 10, 22, 10, 46);
    }
    private final static Calendar twomonthsbefore = Calendar.getInstance();
    static {
        twomonthsbefore.set(2009, 9, 23, 10, 46);
    }
    private final static Calendar yearbefore = Calendar.getInstance();
    static {
        yearbefore.set(2008, 11, 23, 10, 46);
    }
    
    private final static Calendar twoyearbefore = Calendar.getInstance();
    static {
        twoyearbefore.set(2007, 11, 23, 10, 46);
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
        Node facetNavigation = navigation.addNode("hippo:navigation", HippoNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getUUID());
        facetNavigation.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {"hippo:date$year", "hippo:date$month", "hippo:date$day"});
        facetNavigation.setProperty(HippoNodeType.HIPPO_FACETNODENAMES, new String[] {"year", "month", "day"});
        
        session.save();

        int currentYear = start.get(Calendar.YEAR);
        int yearAgo = yearbefore.get(Calendar.YEAR);
        int twoYearsAgo = twoyearbefore.get(Calendar.YEAR);
        int currentMonth = start.get(Calendar.MONTH);
        int currentDay = start.get(Calendar.DAY_OF_MONTH);
        
        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
    
        assertNotNull(node.getNode("year"));
        assertNotNull(node.getNode("year").getNode(String.valueOf(currentYear)));
       
        assertTrue(node.getNode("year").getNode(String.valueOf(currentYear)).getProperty(HippoNodeType.HIPPO_COUNT).getLong() == 7L);
        assertTrue(node.getNode("year").getNode(String.valueOf(yearAgo)).getProperty(HippoNodeType.HIPPO_COUNT).getLong() == 1L);
        assertTrue(node.getNode("year").getNode(String.valueOf(twoYearsAgo)).getProperty(HippoNodeType.HIPPO_COUNT).getLong() == 1L);
        
        assertNotNull(node.getNode("month"));
        assertNotNull(node.getNode("month").getNode(String.valueOf(currentMonth)));
        
        assertNotNull(node.getNode("day"));
        assertNotNull(node.getNode("day").getNode(String.valueOf(currentDay)));
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

        addDateDoc(dateDocs, "datedoc1", start);
        addDateDoc(dateDocs, "datedoc2", onehourbefore);
        addDateDoc(dateDocs, "datedoc3", onedaybefore);
        addDateDoc(dateDocs, "datedoc4", threedaybefore);
        addDateDoc(dateDocs, "datedoc5", monthbefore);
        addDateDoc(dateDocs, "datedoc6", monthandadaybefore);
        addDateDoc(dateDocs, "datedoc7", twomonthsbefore);
        addDateDoc(dateDocs, "datedoc8", yearbefore);
        addDateDoc(dateDocs, "datedoc9", twoyearbefore);
		
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
