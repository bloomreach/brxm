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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.Before;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public class FacetedNavigationRangesTest extends AbstractRangesFacetNavigationTest {

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

    @Ignore
    public void testDateRanges() throws RepositoryException, IOException {
        if(testShouldSkip()) {
            assertTrue(true);
            return;
        }
        
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        String[] ranges = new String[] {"hippo:date$[{name:'today', resolution:'day', begin:0, end:1}, {name:'yesterday', resolution:'day', begin:-1, end:0}]"};
        
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS,ranges);
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "range${hide:'range'}" });

        session.save();

        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        
        assertNotNull(node.getNode("range"));
        assertNotNull(node.getNode("range/today"));
        assertNotNull(node.getNode("range/today/hippo:resultset"));
        assertEquals(2L, node.getNode("range/today").getProperty("hippo:count").getLong());

        assertNotNull(node.getNode("range/yesterday"));
        assertNotNull(node.getNode("range/yesterday/hippo:resultset"));
        assertEquals(1L, node.getNode("range/yesterday").getProperty("hippo:count").getLong());
        
        // because we say:  "range${hide:'range'}" range should not be returned
        assertFalse(node.getNode("range/yesterday").hasNode("range"));
    }


    @Ignore
    public void testDateOpenRange() throws RepositoryException, IOException {
        if(testShouldSkip()) {
            assertTrue(true);
            return;
        }
        
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
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
        createDateStructure(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
        
        String[] facetRanges = {"hippo:price$[" +
                "{name:'less 10.000', resolution:'double', end:10000}," +
                "{name:'10.000 - 20.000', resolution:'double', begin:10000, end:20000}," +
                "{name:'more 20.000', resolution:'double', begin:20000}, " +
                "{name:'all prices', resolution:'double'}" +
                "]"};
        
        facetNavigation.setProperty(
                        FacNavNodeType.HIPPOFACNAV_FACETS,facetRanges);
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "price" });
        
        // below is the order of the result set
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
    public void testFacetRangeOrdering() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());

        String[] facetRanges = { "hippo:price$[" 
                + "{name:'first', resolution:'double', end:10000},"
                + "{name:'second', resolution:'double', begin:10000, end:20000},"
                + "{name:'third', resolution:'double', begin:20000}, "
                + "{name:'fourth', resolution:'double'}" + "]" };
        
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, facetRanges);
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "price" });

        session.save();

        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        // by default, ranges are ordered in order that they are configured, We thus expect first --> second --> third --> fourth
        
        assertNotNull(node.getNode("price"));
        
        
        String[] orderOfNodes = {"first","second","third","fourth", "hippo:resultset"};

        NodeIterator it = node.getNode("price").getNodes();
        int i = 0;
        while(it.hasNext()) {
            assertEquals(it.nextNode().getName(), orderOfNodes[i]);
            i++;
        }
        
        // now test reversed order:
        
        node.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "price${sortby:'config', sortorder:'descending'}" });
        session.save();
        
        node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        
        String[] reversedOrderOfNodes = {"fourth","third","second","first", "hippo:resultset"};

        it = node.getNode("price").getNodes();
        i = 0;
        while(it.hasNext()) {
            assertEquals(it.nextNode().getName(), reversedOrderOfNodes[i]);
            i++;
        }
        
    }
    
    @Test
    public void testLongRanges() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
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
        createDateStructure(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
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

    /*
     * Make sure that timezone work correct with GMT
     */    
    @Test
    public void testTimeZoneDateFacetNavigation() throws RepositoryException, IOException {
        if(testShouldSkip()) {
            assertTrue(true);
            return;
        }
        commonStart();

        Node testNode = session.getRootNode().getNode("test");
        // false thus we do not populate any car
        createDateStructure(testNode, false);
        Node carDocs = testNode.getNode("documents").getNode("datedocs");
        
        // let's populate one car, just after midnight: this should be indexed the correct day (GMT time might result in that 00:24 is still seen
        // as the day before because of the time zone)
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 23);
        
        // Add a car that has date today, at 00:23. It should be found in the facet range 'today'. Because of GMT dates, this is 
        // a valuable test. 00:23 might be yesterday wrt GMT time
        addCar(carDocs, "start", cal, 125000L, 18000.0D, "peugeot");
        session.save();
        
        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getIdentifier());
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS, new String[] {"hippo:date$[{name:'today', resolution:'day', begin:0, end:1},{name:'yesterday', resolution:'day', begin:-1, end:0}]" });
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] {"range" });

        session.save();
        
        facetNavigation = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
      

        assertEquals(1L, facetNavigation.getNode("range").getNode("today").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        assertEquals(0L, facetNavigation.getNode("range").getNode("yesterday").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        
    }

    /*
     * Because week ranges are a bit more complex as they have format overlapping with months indexing, we have a dedicated test for this
     */
    @Test
    public void testWeekDateRanges() throws RepositoryException, IOException {
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        String[] ranges = new String[] {"hippo:date$month" ,"hippo:date$[{name:'today', resolution:'day', begin:0, end:1}, {name:'this week', resolution:'week', begin:0, end:1}, {name:'this month', resolution:'month', begin:0, end:1}]"};
        
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS,ranges);
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "month", "range" });

        session.save();
        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");
        Long count1 = node.getNode("range").getNode("this week").getProperty(HippoNodeType.HIPPO_COUNT).getLong();
        Long count2;
        if (node.hasNode("range/this week/range")) {
            count2 = node.getNode("range").getNode("this week").getNode("range").getNode("this month").getProperty(HippoNodeType.HIPPO_COUNT).getLong();
        } else {
            count2 = 0L;
        }
        // when this week constraint is already chosen, then this month can contain at most as many items (in the beginning of a month, the current 
        // week can contain more items then the month)
        assertTrue(count1 >= count2);
    }

}
