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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.facetnavigation.FacNavNodeType;
import org.junit.Test;


public class FacetedNavigationRangesTest extends AbstractFacetNavigationTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    

    @Test
    public void testDateRanges() throws RepositoryException, IOException {
        if(testShouldSkip()) {
            assertTrue(true);
            return;
        }
        
        commonStart();
        Node testNode = session.getRootNode().getNode("test");
        createDateStructure1(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getUUID());
        String[] ranges = new String[] {"hippo:date$[{name:'today', resolution:'day', begin:0, end:1}, {name:'yesterday', resolution:'day', begin:-1, end:0}]"};
        
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETS,ranges);
        facetNavigation.setProperty(FacNavNodeType.HIPPOFACNAV_FACETNODENAMES, new String[] { "range" });

        session.save();

        Node node = session.getRootNode().getNode("test/facetnavigation/hippo:navigation");

        FacetViewHelper.traverse(node);
        
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
        if(testShouldSkip()) {
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
        createDateStructure1(testNode);

        Node navigation = testNode.addNode("facetnavigation");
        Node facetNavigation = navigation.addNode("hippo:navigation", FacNavNodeType.NT_FACETNAVIGATION);
        facetNavigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents")
                .getUUID());

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

    

}
