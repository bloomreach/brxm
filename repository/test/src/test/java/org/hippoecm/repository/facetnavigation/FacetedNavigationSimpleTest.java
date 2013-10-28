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

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.iterator.NodeIterable;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FacetedNavigationSimpleTest extends RepositoryTestCase {

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
        // assert there is a correct count property:
        assertTrue(node.hasProperty("hippo:count"));
        // there are 5 cars on the root navigation (although car0 does not have facets!)
        assertEquals(5L, node.getProperty("hippo:count").getLong());
        // there must be a resultset with 5 cars:
        assertTrue(node.hasNode("hippo:resultset"));
        assertTrue(node.getNode("hippo:resultset").hasProperty("hippo:count"));
        assertEquals(5L , node.getNode("hippo:resultset").getProperty("hippo:count").getLong());
        assertEquals(5L , node.getNode("hippo:resultset").getNodes().getSize());

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
        assertEquals(4L, node.getNode("hippo:brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(4L, node.getNode("hippo:brand/hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(2L, node.getNode("hippo:brand/peugeot").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(2L, node.getNode("hippo:brand/peugeot/hippo:brand/peugeot").getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());

        // assert some counts are equal to number of nodes in resultset
        assertEquals(node.getNode("hippo:brand/hippo:resultset").getNodes().getSize(), node.getNode("hippo:brand")
                .getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("hippo:brand/peugeot/hippo:resultset").getNodes().getSize(), node.getNode(
                "hippo:brand/peugeot").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("hippo:brand/peugeot/hippo:color/hippo:resultset").getNodes().getSize(), node
                .getNode("hippo:brand/peugeot/hippo:color").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("hippo:brand/peugeot/hippo:product/car/hippo:resultset").getNodes().getSize(), node
                .getNode("hippo:brand/peugeot/hippo:product/car").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("hippo:brand/peugeot/hippo:product/car/hippo:color/grey/hippo:resultset").getNodes()
                .getSize(), node.getNode("hippo:brand/peugeot/hippo:product/car/hippo:color/grey").getProperty(
                HippoNodeType.HIPPO_COUNT).getLong());

    }

    @Test
    public void testFacetNodeNameNavigation() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeSingleValues(testNode);

        testNode.getNode("facetnavigation/hippo:navigation").setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES,
                new String[] { "brand", "color", "product" });

        session.save();

        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        assertNotNull(node);
        // assert some facetednavigation nodes exists
        assertTrue(node.hasNode("brand/peugeot/color/hippo:resultset"));
        assertTrue(node.hasNode("brand/peugeot/color/grey"));
        assertTrue(node.hasNode("brand/peugeot/color/grey/hippo:resultset"));
        assertTrue(node.hasNode("brand/peugeot/product/car"));
        assertTrue(node.hasNode("brand/peugeot/product/car/color/grey"));

        // assert that after iterating the same key-value twice, there are no child nodes below this node:
        assertTrue(node.hasNode("brand/peugeot/brand/peugeot"));
        assertFalse(node.getNode("brand/peugeot/brand/peugeot").hasNodes());

        // assert some counts:
        assertEquals(4L, node.getNode("brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(4L, node.getNode("brand/hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(2L, node.getNode("brand/peugeot").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(2L, node.getNode("brand/peugeot/brand/peugeot").getProperty(HippoNodeType.HIPPO_COUNT).getLong());

        // assert some counts are equal to number of nodes in resultset
        assertEquals(node.getNode("brand/hippo:resultset").getNodes().getSize(), node.getNode("brand").getProperty(
                HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("brand/peugeot/hippo:resultset").getNodes().getSize(), node.getNode("brand/peugeot")
                .getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("brand/peugeot/color/hippo:resultset").getNodes().getSize(), node.getNode(
                "brand/peugeot/color").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("brand/peugeot/product/car/hippo:resultset").getNodes().getSize(), node.getNode(
                "brand/peugeot/product/car").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(node.getNode("brand/peugeot/product/car/color/grey/hippo:resultset").getNodes().getSize(), node
                .getNode("brand/peugeot/product/car/color/grey").getProperty(HippoNodeType.HIPPO_COUNT).getLong());

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
        
        // assert there is a correct count property:
        assertTrue(navigation.hasProperty("hippo:count"));
        // there are 5 cars on the root navigation (although car0 does not have facets!)
        assertEquals(5L, navigation.getProperty("hippo:count").getLong());
        // there must be a resultset with 5 cars:
        assertTrue(navigation.hasNode("hippo:resultset"));
        assertTrue(navigation.getNode("hippo:resultset").hasProperty("hippo:count"));
        assertEquals(5L , navigation.getNode("hippo:resultset").getProperty("hippo:count").getLong());
        assertEquals(5L , navigation.getNode("hippo:resultset").getNodes().getSize());
        
        assertNotNull(navigation.getNode("hippo:brand"));
        assertEquals(4L, navigation.getNode("hippo:brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
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
        String docbase = testNode.getNode("facetnavigation").getIdentifier();
        Node facetselect = testNode.addNode("filtered", HippoNodeType.NT_FACETSELECT);
        facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, docbase);
        facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] { "select" });
        facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:brand" });
        facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "peugeot" });
        session.save();
        session.refresh(false);

        // with filter now:

        assertNotNull(testNode.getNode("filtered"));
        assertNotNull(testNode.getNode("filtered/hippo:navigation"));
        

        Node filteredNavigation = testNode.getNode("filtered/hippo:navigation");
        // assert there is a correct count property:
        assertTrue(filteredNavigation.hasProperty("hippo:count"));
        // there are 2 cars on the root navigation after the filter
        assertEquals(2L, filteredNavigation.getProperty("hippo:count").getLong());
        // there must be a resultset with 2 cars:
        assertTrue(filteredNavigation.hasNode("hippo:resultset"));
        assertTrue(filteredNavigation.getNode("hippo:resultset").hasProperty("hippo:count"));
        assertEquals(2L , filteredNavigation.getNode("hippo:resultset").getProperty("hippo:count").getLong());
        assertEquals(2L , filteredNavigation.getNode("hippo:resultset").getNodes().getSize());

        assertNotNull(filteredNavigation.getNode("hippo:brand"));
        // after filter, only 2 results here!
        assertEquals(2L, filteredNavigation.getNode("hippo:brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        // mercedes should not resolve through filter
        assertFalse(filteredNavigation.hasNode("hippo:brand/mercedes"));

        assertTrue(filteredNavigation.hasNode("hippo:brand/peugeot"));
        assertEquals(2L, filteredNavigation.getNode("hippo:brand/peugeot").getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());
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
        assertEquals(4L, navigation.getNode("hippo:brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());

        // the primary node type counts: note, that there are 5 cars, onyly 4 of them having a brand, hence, 5L hippo:testdocument's 

        assertNotNull(navigation.getNode("jcr:primaryType"));
        assertEquals(5L, navigation.getNode("jcr:primaryType").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        String encodeNodeName = NodeNameCodec.encode("hippo:testcardocument", true);
        assertEquals(5L, navigation.getNode("jcr:primaryType/" + encodeNodeName).getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());
        assertNotNull(navigation.getNode("hippo:brand/peugeot/jcr:primaryType/" + encodeNodeName));
        assertEquals(2L, navigation.getNode("hippo:brand/peugeot/jcr:primaryType/" + encodeNodeName).getProperty(
                HippoNodeType.HIPPO_COUNT).getLong());

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
        assertNotNull(navigation.getNode("hippo:date/" + globalCal.getTimeInMillis()));
        assertEquals(4L, navigation.getNode("hippo:date/" + globalCal.getTimeInMillis()).getProperty(
                HippoNodeType.HIPPO_COUNT).getLong());

    }

    /**
     * Tests CMS7-5026: facetnavigation on hippostd:state should include 'draft' state
     *
     * @throws RepositoryException
     * @throws IOException
     */
    @Test
    public void testFacetsOnHippoState() throws RepositoryException, IOException {
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        createDocumentsWithStateStructure(testNode);
        createFacetNodeHippoState(testNode);
        session.save();

        Node navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        Node stateNode = navigation.getNode(HippoStdNodeType.HIPPOSTD_STATE);
        assertNotNull(stateNode);
        assertEquals(3L, stateNode.getProperty(HippoNodeType.HIPPO_COUNT).getLong());

        Node published = stateNode.getNode("published");
        assertNotNull(published);
        assertEquals(1L, published.getProperty(HippoNodeType.HIPPO_COUNT).getLong());

        Node unpublished = stateNode.getNode("unpublished");
        assertNotNull(unpublished);
        assertEquals(1L, unpublished.getProperty(HippoNodeType.HIPPO_COUNT).getLong());

        Node draft = stateNode.getNode("draft");
        assertNotNull(draft);
        assertEquals(1L, draft.getProperty(HippoNodeType.HIPPO_COUNT).getLong());
    }

    @Test
    public void testSortResultSetOnStringProperty() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeSingleValues(testNode);

        session.save();

        Node navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        navigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTBY, new String[] { "hippo:brand", "hippo:color" });
        navigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTORDER, new String[] { "descending", "descending" });
        session.save();
        navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        
        // the resultset shoult be sorted on {hippo:brand (descending) and then on hippo:color (descending), see createFacetNodeSingleValues}
        
        // check directly for resultset below facet navigation
        {
            NodeIterator it = navigation.getNode("hippo:resultset").getNodes();
            String prevBrandValue = null;
            while (it.hasNext()) {
                Node n = it.nextNode();
                if(n.hasProperty("hippo:brand")) {
                    String brand = n.getProperty("hippo:brand").getString();
                    if (prevBrandValue != null) {
                        int compare = prevBrandValue.compareTo(brand);
                        // if sorted correctly, compare must be >= 0
                        assertTrue("Sorting of resultset failed", compare >= 0);
                    }
                    prevBrandValue = brand;
                }
            }
        }
        // check for first facet
        {
            NodeIterator it = navigation.getNode("hippo:brand/hippo:resultset").getNodes();
            String prevBrandValue = null;
            while (it.hasNext()) {
                Node n = it.nextNode();
                String brand = n.getProperty("hippo:brand").getString();
                if (prevBrandValue != null) {
                    int compare = prevBrandValue.compareTo(brand);
                    // if sorted correctly, compare must be >= 0
                    assertTrue("Sorting of resultset failed", compare >= 0);
                }
                prevBrandValue = brand;
            }
        }
    }

    @Test
    public void testSortResultSetOnLongProperty() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeSingleValues(testNode);

        session.save();

        Node navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        navigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTBY, new String[] { "hippo:travelled" });
        navigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTORDER, new String[] { "descending" });
        session.save();
        navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        // the resultset shoult be sorted on {hippo:brand (descending) and then on hippo:color (descending), see createFacetNodeSingleValues}
        NodeIterator it = navigation.getNode("hippo:brand/hippo:resultset").getNodes();
        long prevLongValue = Long.MAX_VALUE;
        while (it.hasNext()) {
            Node n = it.nextNode();
            long travelled = n.getProperty("hippo:travelled").getLong();
            // if sorted correctly (descending), travelled must be equal or larger then prev long
            assertTrue("Sorting of resultset failed", travelled <= prevLongValue);
            prevLongValue = travelled;
        }
    }

    @Test
    public void testSortResultSetOnDoubleProperty() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);
        createFacetNodeSingleValues(testNode);

        session.save();

        Node navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        navigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTBY, new String[] { "hippo:price" });
        navigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTORDER, new String[] { "descending" });
        session.save();
        navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        // the resultset shoult be sorted on {hippo:brand (descending) and then on hippo:color (descending), see createFacetNodeSingleValues}
        NodeIterator it = navigation.getNode("hippo:brand/hippo:resultset").getNodes();
        double prevDoubleValue = Double.MAX_VALUE;
        while (it.hasNext()) {
            Node n = it.nextNode();
            double price = n.getProperty("hippo:price").getDouble();
            // if sorted correctly (descending), travelled must be equal or larger then prev long
            assertTrue("Sorting of resultset failed", price <= prevDoubleValue);
            prevDoubleValue = price;
        }
    }

    @Test
    public void testLimitResultSet() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);

        createFacetNodeSingleValues(testNode);
        session.save();

        Node navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        assertEquals(4L, navigation.getNode("hippo:brand/hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());

        // and now, add a limit to the resultset!
        navigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETLIMIT, 1L);
        session.save();

        navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        // And now the resultset should be just 1 as we have set the limit to 1
        assertEquals(1L, navigation.getNode("hippo:brand/hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT)
                .getLong());

    }
    
    /*
     * For an empty facet, we expect the following behavior: the count for the facet includes the item where the
     * facet value is empty, and the resultset does so as well. The empty facet value however does not return as a facet node of course,
     * hence, all the counts of all the facet values combined can be lower then the count from the facet node itself: for example:
     * 
     * hippo:brand (5)
     *    |- peugeot (2)
     *    |- volkswagen (1)
     *    |- mercedes (1)
     *    `- hippo:resultset (5)
     *    
     * would be the result if one car has an empty value for brand (thus, property exists, but is empty)
     */
    @Test
    public void testWithEmptyFacetValue() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);

        Node cars = testNode.getNode("documents/cars");
        
        // car with emtpy facet value for brand
        Node car = cars.addNode("car_emptybrand", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car_emptybrand", "hippo:testcardocument");
        car.addMixin("mix:versionable");
        car.setProperty("hippo:brand", "");
        car.setProperty("hippo:color", "grey");
        car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
        car.setProperty("hippo:price", 12000.0D);
        car.setProperty("hippo:travelled", 122000L);
        
        createFacetNodeSingleValues(testNode);
        
        session.save();

        Node navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        assertEquals(5L, navigation.getNode("hippo:brand/hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(5L, navigation.getNode("hippo:brand").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
        long allFacetValueCounts = 0;
        NodeIterator it = navigation.getNode("hippo:brand").getNodes();
        while(it.hasNext()) {
            Node child = it.nextNode();
            // we only count facetvalues, not the resultset count ofcourse
            if(child.isNodeType(FacNavNodeType.NT_FACETSUBNAVIGATION)) {
                allFacetValueCounts += child.getProperty(HippoNodeType.HIPPO_COUNT).getLong();
            }
        }
        
        // allFacetValueCounts combined is still 1 less then the resultset because we have one car with an empty value for brand
        assertTrue(allFacetValueCounts + 1 == navigation.getNode("hippo:brand/hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
       

    }
    
    @Test
    public void testMultipleScopesFacetNavigation() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createSimpleStructure1(testNode);

        createFacetNodeSingleValues(testNode);
        
        session.save();
        Node navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
 
        
        // reset the docbase to multiple docbases (comma seperated list):
        // we only have docbase of car1 and car2
        String docbases = new String();
        
        docbases = testNode.getNode("documents/cars/car1").getIdentifier() + "," + testNode.getNode("documents/cars/car2").getIdentifier();
        
        navigation.setProperty(HippoNodeType.HIPPO_DOCBASE, docbases);
        
        session.save();
        
        navigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        
        // we now only have car 1 and car 2 in our results
        assertEquals(2L, navigation.getNode("hippo:brand/hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertTrue(navigation.getNode("hippo:brand/hippo:resultset").hasNode("car1"));
        assertTrue(navigation.getNode("hippo:brand/hippo:resultset").hasNode("car2"));
        assertFalse(navigation.getNode("hippo:brand/hippo:resultset").hasNode("car3"));
        assertFalse(navigation.getNode("hippo:brand/hippo:resultset").hasNode("car4"));
    }

    private void commonStart() throws RepositoryException {
        session.getRootNode().addNode("test");
        session.save();
    }

    private void createSimpleStructure1(Node test) throws RepositoryException {
        Node documents = test.addNode("documents", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        Node cars = documents.addNode("cars", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        /*
         * car 0
         * car that has no facets, so should not be visible at all in facet
         */
        Node car = cars.addNode("car0", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car0", "hippo:testcardocument");
        car.addMixin("mix:versionable");

        // car 1
        car = cars.addNode("car1", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car1", "hippo:testcardocument");
        car.addMixin("mix:versionable");
        car.setProperty("hippo:brand", "mercedes");
        car.setProperty("hippo:color", "grey");
        car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
        car.setProperty("hippo:price", 12000.0D);
        car.setProperty("hippo:travelled", 122000L);

        // car 2
        car = cars.addNode("car2", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car2", "hippo:testcardocument");
        car.addMixin("mix:versionable");
        car.setProperty("hippo:brand", "volkswagen");
        car.setProperty("hippo:color", "grey");
        car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
        car.setProperty("hippo:price", 13000.0D);
        car.setProperty("hippo:travelled", 129000L);

        // car 3
        car = cars.addNode("car3", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car3", "hippo:testcardocument");
        car.addMixin("mix:versionable");
        car.setProperty("hippo:brand", "peugeot");
        car.setProperty("hippo:color", "blue");
        car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
        car.setProperty("hippo:price", 11900.0D);
        car.setProperty("hippo:travelled", 99000L);

        test.save();

        // car 4
        car = cars.addNode("car4", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car4", "hippo:testcardocument");
        car.addMixin("mix:versionable");

        // add a facetselect to car 1
        String docbase = cars.getNode("car1").getIdentifier();
        Node facetselect = car.addNode("car1", HippoNodeType.NT_FACETSELECT);
        facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, docbase);
        facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
        facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});
        facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});

        car.setProperty("hippo:brand", "peugeot");
        car.setProperty("hippo:color", "grey");
        car.setProperty("hippo:product", "car");
        car.setProperty("hippo:date", globalCal);
        car.setProperty("hippo:price", 14000.0D);
        car.setProperty("hippo:travelled", 72340L);

    }

    private void createSimpleStructure2(Node test) throws RepositoryException {
        Node documents = test.addNode("documents", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        Node cars = documents.addNode("cars", "nt:unstructured");

        // car 1
        Node car = cars.addNode("car1", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car1", "hippo:testtagdocument");
        car.addMixin("mix:versionable");
        String[] tags1 = { "mercedes", "expensive", "lease" };
        car.setProperty("tags", tags1);
        // car 2
        car = cars.addNode("car2", "hippo:handle");
        car.addMixin("hippo:hardhandle");
        car = car.addNode("car2", "hippo:testtagdocument");
        car.addMixin("mix:versionable");
        String[] tags2 = { "toyota", "environment", "economical", "lease" };
        car.setProperty("tags", tags2);
    }

    private void createDocumentsWithStateStructure(Node test) throws RepositoryException {
        Node documents = test.addNode("documents", "nt:unstructured");
        documents.addMixin("mix:referenceable");
        Node myDocs = documents.addNode("mydocs", "nt:unstructured");

        addDocumentWithState(myDocs, "doc1", "published");
        addDocumentWithState(myDocs, "doc2", "unpublished");
        addDocumentWithState(myDocs, "article3", "draft");
    }

    private void addDocumentWithState(Node node, String name, String state) throws RepositoryException {
        Node article = node.addNode(name, "hippo:handle");
        article.addMixin("hippo:hardhandle");
        article = article.addNode(name, "hippo:document");
        article.addMixin("mix:versionable");
        article.addMixin(HippoStdNodeType.NT_PUBLISHABLE);
        article.setProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
    }

    private void createFacetNodeSingleValues(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node.addMixin("mix:referenceable");
        node = node.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getNode("/test/documents").getIdentifier());
        node.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:brand", "hippo:color", "hippo:product" });

    }
    

    private void createFacetNodeMultiValue(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node = node.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        node.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "tags" });
    }

    private void createFacetNodeWithPrimaryType(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node = node.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        node.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "jcr:primaryType", "hippo:brand" });
    }

    private void createFacetNodeDateValue(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node = node.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        node.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { "hippo:date" });
    }

    private void createFacetNodeHippoState(Node node) throws RepositoryException {
        node = node.addNode("facetnavigation");
        node = node.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        node.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] { HippoStdNodeType.HIPPOSTD_STATE });
    }

}
