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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FacetedNavigationSimpleTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final Calendar globalCal = Calendar.getInstance();
    
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
    public void testDirectSingleValuedFacetNavigation() throws RepositoryException, IOException {
    	commonStart();
    	
    	Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeSingleValues(testNode);
        session.save();
        
    	Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
    	
    	assertNotNull(node);
    	// assert some facetednavigation nodes exists
        assertTrue(node.hasNode("hippo:brand/peugeot/hippo:color/hippo:resultset"));
        assertTrue(node.hasNode("hippo:brand/peugeot/hippo:color/grey"));
        assertTrue(node.hasNode("hippo:brand/peugeot/hippo:color/grey/hippo:resultset"));
        assertTrue(node.hasNode("hippo:brand/peugeot/hippo:product/car"));
        assertTrue(node.hasNode("hippo:brand/peugeot/hippo:product/car/hippo:color/grey"));
        
        // assert that after iterating the same key-value twice, there are no child nodes below this node:
        assertTrue(node.hasNode("hippo:brand/peugeot/hippo:brand/peugeot"));
        assertFalse(node.getNode("hippo:brand/peugeot/hippo:brand/peugeot").hasNodes());
    	
        // assert some counts:
        assertEquals(4L,node.getNode("hippo:brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(4L,node.getNode("hippo:brand/hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong()) ;
        assertEquals(2L,node.getNode("hippo:brand/peugeot").getProperty(HippoNodeType.HIPPO_COUNT).getLong()) ;
        assertEquals(2L,node.getNode("hippo:brand/peugeot/hippo:brand/peugeot").getProperty(HippoNodeType.HIPPO_COUNT).getLong()) ;
        
        // assert some counts are equal to number of nodes in resultset
        assertEquals(node.getNode("hippo:brand/hippo:resultset").getNodes().getSize(),node.getNode("hippo:brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("hippo:brand/peugeot/hippo:resultset").getNodes().getSize(),node.getNode("hippo:brand/peugeot").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("hippo:brand/peugeot/hippo:color/hippo:resultset").getNodes().getSize(),node.getNode("hippo:brand/peugeot/hippo:color").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("hippo:brand/peugeot/hippo:product/car/hippo:resultset").getNodes().getSize(),node.getNode("hippo:brand/peugeot/hippo:product/car").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("hippo:brand/peugeot/hippo:product/car/hippo:color/grey/hippo:resultset").getNodes().getSize(),node.getNode("hippo:brand/peugeot/hippo:product/car/hippo:color/grey").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
    }
    
    @Test
    public void testDirectMultiValuedFacetNavigation() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure2(testNode);
        createFacetNodeMultiValue(testNode);
        session.save();
        Node node = testNode.getNode("facetnavigation/hippo:navigation");
        assertNotNull(node);
        
        // assert some facetednavigation nodes exists (note, only one multivalued property)
        assertTrue(node.hasNode("tags/lease"));
        assertTrue(node.hasNode("tags/lease/tags/lease"));
        assertTrue(node.hasNode("tags/toyota"));
        
        assertTrue(node.hasNode("tags/toyota/tags/economical/tags/economical"));
        assertTrue(node.hasNode("tags/toyota/tags/economical/tags/toyota"));
        
        // when accessing an already use combination, assure no child node are present:

        assertTrue(node.getNode("tags/toyota/tags/economical/tags/economical").getNodes().getSize() == 0);
        assertTrue(node.getNode("tags/toyota/tags/economical/tags/toyota").getNodes().getSize() == 0);
       
    }
    
    @Test
    public void testInheritFilterFacetNavigation() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeSingleValues(testNode);
        session.save();
        
        
        Node navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        
        // without filter:
        assertNotNull(navigation.getNode("hippo:brand"));
        assertEquals(4L,navigation.getNode("hippo:brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertNotNull(navigation.getNode("hippo:brand/mercedes"));
        assertNotNull(navigation.getNode("hippo:brand/peugeot/hippo:color/hippo:resultset"));
        assertNotNull(navigation.getNode("hippo:brand/peugeot/hippo:color/hippo:resultset/car4"));
        // car 4 has a link to car 1
        assertNotNull(navigation.getNode("hippo:brand/peugeot/hippo:color/hippo:resultset/car4/car1"));
        assertNotNull(navigation.getNode("hippo:brand/peugeot/hippo:color/hippo:resultset/car4/car1/car1"));
        
        /*
         * let's create a facetselect with filter, that points to the faceted navigation.
         * The criteria is, that we only want to see brand = peugeot. This needs to be 
         * reflected in the count numbers & resultset and facetvalue in the faceted navigation as well
         */  
        
        testNode = session.getRootNode().getNode("test");
        String docbase = testNode.getNode("facetnavigation").getUUID();
        Node facetselect = testNode.addNode("filtered", HippoNodeType.NT_FACETSELECT);
        facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, docbase);
        facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[]{"select"});
        facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[]{"hippo:brand"});
        facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[]{"peugeot"});
        session.save();
        session.refresh(false);
        
        // with filter now:

        assertNotNull(testNode.getNode("filtered"));
        assertNotNull(testNode.getNode("filtered/hippo:navigation"));
        
        Node filteredNavigation = testNode.getNode("filtered/hippo:navigation");
        assertNotNull(filteredNavigation.getNode("hippo:brand"));
        // after filter, only 2 results here!
        assertEquals(2L,filteredNavigation.getNode("hippo:brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        // mercedes should not resolve through filter
        assertFalse(filteredNavigation.hasNode("hippo:brand/mercedes"));

        assertTrue(filteredNavigation.hasNode("hippo:brand/peugeot"));
        assertEquals(2L, filteredNavigation.getNode("hippo:brand/peugeot").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertNotNull(filteredNavigation.getNode("hippo:brand/peugeot/hippo:color/hippo:resultset/car4"));
        // car 4 has a link to car 1 so the handle is visible
        assertNotNull(filteredNavigation.getNode("hippo:brand/peugeot/hippo:color/hippo:resultset/car4/car1"));
        // because of the original filter, car1 document which is brand mercedes is not allowed to be visible

        assertFalse(filteredNavigation.hasNode("hippo:brand/peugeot/hippo:color/hippo:resultset/car4/car1/car1"));
    }
    
    
    
    @Test
    public void testFacetsOnNodeType() throws RepositoryException, IOException {
        commonStart();
        
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeWithPrimaryType(testNode);
        session.save();
        
        Node navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        // the brand count:
        assertNotNull(navigation.getNode("hippo:brand"));
        assertEquals(4L,navigation.getNode("hippo:brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
        // the primary node type counts: note, that there are 5 cars, onyly 4 of them having a brand, hence, 5L hippo:testdocument's 
        
        assertNotNull(navigation.getNode("jcr:primaryType"));
        assertEquals(5L,navigation.getNode("jcr:primaryType").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        String encodeNodeName = NodeNameCodec.encode("hippo:testcardocument", true);
        assertEquals(5L,navigation.getNode("jcr:primaryType/"+encodeNodeName).getProperty(HippoNodeType.HIPPO_COUNT).getLong());  
        assertNotNull(navigation.getNode("hippo:brand/peugeot/jcr:primaryType/"+encodeNodeName));
        assertEquals(2L,navigation.getNode("hippo:brand/peugeot/jcr:primaryType/"+encodeNodeName).getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
        session.save();
    }
    
    @Test
    public void testFacetsOnDateValue() throws RepositoryException, IOException {
        commonStart();
        
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeDateValue(testNode);
        session.save();
    
        // we must have a node "/hippo:navigation/hippo:date/" + globalCal.getTimeInMillis() as date nodes are displayed in millisec and there should be 4 of them
        
        Node navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
      
        assertNotNull(navigation.getNode("hippo:date"));
        assertNotNull(navigation.getNode("hippo:date/"+globalCal.getTimeInMillis()));
        assertEquals(4L,navigation.getNode("hippo:date/"+globalCal.getTimeInMillis()).getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
    }
    
    private void commonStart() throws RepositoryException{
    	session.getRootNode().addNode("test");
    	session.save();
    }
    
	private void createSimpleStructure1(Node test) throws RepositoryException {
    	Node documents = test.addNode("documents","nt:unstructured");
    	documents.addMixin("mix:referenceable");
		Node cars = documents.addNode("cars","nt:unstructured");
		documents.addMixin("mix:referenceable");
		/*
		 * car 0
		 * car that has no facets, so should not be visible at all in facet
		 */ 
    	Node car = cars.addNode("car0","hippo:handle");
    	car.addMixin("hippo:hardhandle");
    	car = car.addNode("car0","hippo:testcardocument");
    	car.addMixin("hippo:harddocument");
    	
    	
		// car 1
    	car  = cars.addNode("car1","hippo:handle");
    	car.addMixin("hippo:hardhandle");
        car = car.addNode("car1","hippo:testcardocument");
    	car.addMixin("hippo:harddocument");
    	car.setProperty("hippo:brand", "mercedes");
    	car.setProperty("hippo:color", "grey");
    	car.setProperty("hippo:product", "car");
    	car.setProperty("hippo:date", globalCal);
    	
    	// car 2
    	car  = cars.addNode("car2","hippo:handle");
        car.addMixin("hippo:hardhandle");
    	car = car.addNode("car2","hippo:testcardocument");
    	car.addMixin("hippo:harddocument");
    	car.setProperty("hippo:brand", "volkswagen");
    	car.setProperty("hippo:color", "grey");
    	car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
    	
    	// car 3
    	car  = cars.addNode("car3","hippo:handle");
        car.addMixin("hippo:hardhandle");
    	car = car.addNode("car3","hippo:testcardocument");
    	car.addMixin("hippo:harddocument");
    	car.setProperty("hippo:brand", "peugeot");
    	car.setProperty("hippo:color", "blue");
    	car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
    	
    	test.save();
    	
    	// car 4
    	car  = cars.addNode("car4","hippo:handle");
        car.addMixin("hippo:hardhandle");
    	car = car.addNode("car4","hippo:testcardocument");
    	car.addMixin("hippo:harddocument");
    	
    	// add a facetselect to car 1
    	String docbase = cars.getNode("car1").getUUID();
    	Node facetselect = car.addNode("car1", HippoNodeType.NT_FACETSELECT);
    	facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, docbase);
        facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[]{});
        facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[]{});
        facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[]{});
    	
    	car.setProperty("hippo:brand", "peugeot");
    	car.setProperty("hippo:color", "grey");
    	car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
    	
	}
	
	private void createSimpleStructure2(Node test) throws RepositoryException {
	    Node documents = test.addNode("documents","nt:unstructured");
        documents.addMixin("mix:referenceable");
        Node cars = documents.addNode("cars","nt:unstructured");
        
        // car 1
        Node car  = cars.addNode("car1","hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car1","hippo:testtagdocument");
        car.addMixin("hippo:harddocument");
        String[] tags1 = {"mercedes", "expensive", "lease"};
        car.setProperty("tags",tags1);
        // car 2
        car  = cars.addNode("car2","hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car2","hippo:testtagdocument");
        car.addMixin("hippo:harddocument");
        String[] tags2 = {"toyota", "environment", "economical", "lease"};
        car.setProperty("tags", tags2);
	}

	private void createFacetNodeSingleValues(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node.addMixin("mix:referenceable");
        node = node.addNode("hippo:navigation", HippoNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getUUID());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:brand", "hippo:color", "hippo:product" });
    }
	
	private void createFacetNodeMultiValue(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node = node.addNode("hippo:navigation", HippoNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getUUID());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "tags"});
    }
	
	private void createFacetNodeWithPrimaryType(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node = node.addNode("hippo:navigation", HippoNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getUUID());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "jcr:primaryType", "hippo:brand"});
    }
	
	private void createFacetNodeDateValue(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node = node.addNode("hippo:navigation", HippoNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getUUID());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {"hippo:date"});
    }
    
	 
 
}
