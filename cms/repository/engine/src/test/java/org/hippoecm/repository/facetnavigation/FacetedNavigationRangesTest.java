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
import org.hippoecm.repository.query.lucene.HippoDateTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FacetedNavigationRangesTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final static Calendar start = Calendar.getInstance();
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
    public void testDateRanges() throws RepositoryException, IOException {
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
        if ((timeMillisSecAtNoon - start.getTimeInMillis()) < 300 * 1000) {
            // we do not run this unit test, as around the swapping of a day, there might be unexpected results, as during the test, the 
            // calendar may change to the next day, leading to wrong expectations
            assertTrue(true);
            return;
        }

        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure1(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getUUID());
        facetNavigation
                .setProperty(
                        FacNavNodeType.HIPPOFACNAV_FACETS,
                        new String[] { "hippo:date$[{name:'yesterday', resolution:'day', begin:-1, end:0}, {name:'today', resolution:'day', begin:0, end:1}]" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "range" });

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

    @Test
    public void testDateOpenRange() throws RepositoryException, IOException {
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
        if ((timeMillisSecAtNoon - start.getTimeInMillis()) < 300 * 1000) {
            // we do not run this unit test, as around the swapping of a day, there might be unexpected results, as during the test, the 
            // calendar may change to the next day, leading to wrong expectations
            assertTrue(true);
            return;
        }

        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure1(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getUUID());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS,
                new String[] { "hippo:date$[{name:'before today', resolution:'day', end:0}]" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "range" });

        session.save();
        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        assertNotNull(node.getNode("range"));
        assertEquals(9L, node.getNode("range").getProperty("hippo:count").getLong());
        assertNotNull(node.getNode("range/before today"));
        assertNotNull(node.getNode("range/before today/hippo:resultset"));
        assertEquals(7L, node.getNode("range/before today").getProperty("hippo:count").getLong());
        assertEquals(7L, node.getNode("range/before today/hippo:resultset").getProperty("hippo:count").getLong());

    }

    @Test
    public void testDoubleRanges() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure1(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getUUID());
        facetNavigation
                .setProperty(
                        FacNavNodeType.HIPPOFACNAV_FACETS,
                        new String[] { "hippo:price$[{name:'less 10.000', resolution:'double', end:10000},{name:'10.000 - 20.000', resolution:'double', begin:10000, end:20000},{name:'more 20.000', resolution:'double', begin:20000}, {name:'all prices', resolution:'double'}]" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "price" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTBY, new String[] { "hippo:price" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTORDER, new String[] { "descending" });

        session.save();

        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        assertNotNull(node.getNode("price"));
        assertEquals(9L, node.getNode("price").getProperty("hippo:count").getLong());
        assertNotNull(node.getNode("price/all prices"));
        assertNotNull(node.getNode("price/all prices/hippo:resultset"));
        assertEquals(9L, node.getNode("price/all prices").getProperty("hippo:count").getLong());
        assertEquals(9L, node.getNode("price/all prices/hippo:resultset").getProperty("hippo:count").getLong());

        // the exact used combi again should be a leaf node
        assertFalse(node.getNode("price/all prices/price/all prices").hasNodes());

        assertEquals(9L, node.getNode("price/all prices/price/all prices").getProperty("hippo:count").getLong());
        assertEquals(4L, node.getNode("price/all prices/price/less 10.000").getProperty("hippo:count").getLong());
        assertEquals(3L, node.getNode("price/all prices/price/10.000 - 20.000").getProperty("hippo:count").getLong());
        assertEquals(2L, node.getNode("price/all prices/price/more 20.000").getProperty("hippo:count").getLong());
        assertEquals(2L, node.getNode("price/all prices/price/more 20.000/price/all prices").getProperty("hippo:count")
                .getLong());

        // an used combi should result in a leaf node
        assertFalse(node.getNode("price/all prices/price/more 20.000/price/all prices").hasNodes());
        // there are no items less then 10.000 if they before are filtered for items for more then 20.000
        assertEquals(0L, node.getNode("price/all prices/price/more 20.000/price/less 10.000")
                .getProperty("hippo:count").getLong());

    }

    @Test
    public void testLongRanges() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure1(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getUUID());
        facetNavigation
                .setProperty(
                        FacNavNodeType.HIPPOFACNAV_FACETS,
                        new String[] { "hippo:travelled$[{name:'travelled < 50.000', resolution:'long', end:50000},{name:'travelled 50.000-100.000', resolution:'long', begin:50000, end:100000} ,{name:'travelled > 100.000', resolution:'long', begin:100000} ,{name:'all distances', resolution:'long'}]" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "travelled" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTBY, new String[] { "hippo:travelled" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTORDER, new String[] { "descending" });

        session.save();
        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        assertNotNull(node.getNode("travelled"));
        assertEquals(9L, node.getNode("travelled").getProperty("hippo:count").getLong());
        assertNotNull(node.getNode("travelled/all distances"));
        assertNotNull(node.getNode("travelled/all distances/hippo:resultset"));
        assertEquals(9L, node.getNode("travelled/all distances").getProperty("hippo:count").getLong());
        assertEquals(9L, node.getNode("travelled/all distances/hippo:resultset").getProperty("hippo:count").getLong());

        // the exact used combi again should be a leaf node
        assertFalse(node.getNode("travelled/all distances/travelled/all distances").hasNodes());

        assertEquals(9L, node.getNode("travelled/all distances/travelled/all distances").getProperty("hippo:count")
                .getLong());
        assertEquals(6L, node.getNode("travelled/all distances/travelled/travelled > 100.000").getProperty(
                "hippo:count").getLong());
        assertEquals(2L, node.getNode("travelled/all distances/travelled/travelled 50.000-100.000").getProperty(
                "hippo:count").getLong());

        // there are no items less then 50.000 if they before are filtered for items for more then 100.000
        assertEquals(0L, node.getNode(
                "travelled/all distances/travelled/travelled > 100.000/travelled/travelled < 50.000").getProperty(
                "hippo:count").getLong());

    }

    @Test
    public void testStringRanges() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure1(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getUUID());
        StringBuilder ranges = new StringBuilder();
        char current = 'a';
        char next = 'b';
        ranges.append("{name:'" + current + "', resolution:'string', lower:'" + current + "', upper:'" + next + "'}");
        while (current < 'z') {
            current++;
            next++;
            ranges.append(",{name:'" + current + "', resolution:'string', lower:'" + current + "', upper:'" + next
                    + "'}");
        }
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS,
                new String[] { "hippo:brand$[{name:'all', resolution:'string'}, " + ranges.toString() + "]" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "brand" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTBY, new String[] { "hippo:price" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETSORTORDER, new String[] { "descending" });

        session.save();
        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        node.getNode("brand/all/hippo:resultset");

        assertNotNull(node.getNode("brand"));
        assertEquals(9L, node.getNode("brand").getProperty("hippo:count").getLong());
        assertNotNull(node.getNode("brand/all"));
        assertNotNull(node.getNode("brand/all/hippo:resultset"));
        assertEquals(9L, node.getNode("brand/all").getProperty("hippo:count").getLong());
        assertEquals(9L, node.getNode("brand/all/hippo:resultset").getProperty("hippo:count").getLong());
        assertEquals(3L, node.getNode("brand/a").getProperty("hippo:count").getLong());
        assertEquals(3L, node.getNode("brand/a/hippo:resultset").getProperty("hippo:count").getLong());
        assertEquals(3L, node.getNode("brand/a/brand/all").getProperty("hippo:count").getLong());
        assertEquals(0L, node.getNode("brand/a/brand/b").getProperty("hippo:count").getLong());

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
        addCar(dateDocs, "start", start, 125000L, 18000.0D, "peugeot");
        addCar(dateDocs, "onehourbefore", onehourbefore, 112000L, 12125.0D, "peugeot");
        addCar(dateDocs, "onedaybefore", onedaybefore, 92000L, 9156.0D, "audi");
        addCar(dateDocs, "threedaybefore", threedaybefore, 63000L, 22345.0D, "mercedes");
        addCar(dateDocs, "monthbefore", monthbefore, 119000L, 13456.0D, "toyota");
        addCar(dateDocs, "monthandadaybefore", monthandadaybefore, 134000L, 6787.0D, "audi");
        addCar(dateDocs, "twomonthsbefore", twomonthsbefore, 232000L, 4125.0D, "alfa romeo");
        addCar(dateDocs, "yearbefore", yearbefore, 12200L, 52125.0D, "bmw");
        addCar(dateDocs, "twoyearbefore", twoyearbefore, 152000L, 1225.0D, "bentley");
        test.save();
    }

    private void addCar(Node dateDocs, String name, Calendar cal, long travelled, double price, String brand)
            throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException,
            VersionException, ConstraintViolationException, RepositoryException {
        Node carDoc = dateDocs.addNode(name, "hippo:handle");
        carDoc.addMixin("hippo:hardhandle");
        carDoc = carDoc.addNode(name, "hippo:testcardocument");
        carDoc.addMixin("hippo:harddocument");
        carDoc.setProperty("hippo:date", cal);
        carDoc.setProperty("hippo:travelled", travelled);
        carDoc.setProperty("hippo:price", price);
        carDoc.setProperty("hippo:brand", brand);
    }

}
