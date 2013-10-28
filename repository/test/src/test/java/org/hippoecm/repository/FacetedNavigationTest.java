/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.io.IOException;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;

import org.junit.*;
import static org.junit.Assert.*;

public class FacetedNavigationTest extends FacetedNavigationAbstractTest {

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
    public void testTraversal() throws RepositoryException, IOException {
        commonStart();
        traverse(getSearchNode());
        commonEnd();
    }

    @Test
    public void testCounts() throws RepositoryException, IOException {
        commonStart(500);
        /**
         * Test with 500 results in:
            /test/navigation/xyz    505
            /test/navigation/xyz/x1 166
            /test/navigation/xyz/x1/y1      56
            /test/navigation/xyz/x1/y1/z2   21
            /test/navigation/xyz/x1/y1/z2/hippo:resultset   21
            /test/navigation/xyz/x1/y1/z1   18
            /test/navigation/xyz/x1/y1/z1/hippo:resultset   18
            /test/navigation/xyz/x1/y1/hippo:resultset      56
            /test/navigation/xyz/x1/y2      53
            /test/navigation/xyz/x1/y2/z2   15
            /test/navigation/xyz/x1/y2/z2/hippo:resultset   15
            /test/navigation/xyz/x1/y2/z1   14
            /test/navigation/xyz/x1/y2/z1/hippo:resultset   14
            /test/navigation/xyz/x1/y2/hippo:resultset      53
            /test/navigation/xyz/x1/hippo:resultset 166
            /test/navigation/xyz/x2 156
            /test/navigation/xyz/x2/y1      61
            /test/navigation/xyz/x2/y1/z1   24
            /test/navigation/xyz/x2/y1/z1/hippo:resultset   24
            /test/navigation/xyz/x2/y1/z2   17
            /test/navigation/xyz/x2/y1/z2/hippo:resultset   17
            /test/navigation/xyz/x2/y1/hippo:resultset      61
            /test/navigation/xyz/x2/y2      51
            /test/navigation/xyz/x2/y2/z2   16
            /test/navigation/xyz/x2/y2/z2/hippo:resultset   16
            /test/navigation/xyz/x2/y2/z1   15
            /test/navigation/xyz/x2/y2/z1/hippo:resultset   15
            /test/navigation/xyz/x2/y2/hippo:resultset      51
            /test/navigation/xyz/x2/hippo:resultset 156
            /test/navigation/xyz/hippo:resultset    505
         */
        
        check("/test/navigation/xyz/x1", 1, 0, 0);
        check("/test/navigation/xyz/x1/y1", 1, 1, 0);
        check("/test/navigation/xyz/x1/y1/z2", 1, 1, 2);
        check("/test/navigation/xyz/x1/y1/z1", 1, 1, 1);
        check("/test/navigation/xyz/x1/y2", 1, 2, 0);
        check("/test/navigation/xyz/x1/y2/z2", 1, 2, 2);
        check("/test/navigation/xyz/x1/y2/z1", 1, 2, 1);
        check("/test/navigation/xyz/x2", 2, 0, 0);
        check("/test/navigation/xyz/x2/y1", 2, 1, 0);
        check("/test/navigation/xyz/x2/y1/z1", 2, 1, 1);
        check("/test/navigation/xyz/x2/y1/z2", 2, 1, 2);
        check("/test/navigation/xyz/x2/y2", 2, 2, 0);
        check("/test/navigation/xyz/x2/y2/z2", 2, 2, 2);
        check("/test/navigation/xyz/x2/y2/z1", 2, 2, 1);
        
    }

    @Test
    public void testGetItemFromSession() throws RepositoryException {
        commonStart();
        
        String basePath = "/test/navigation/xyz/x2/y1/z2";
        Item item = session.getItem(basePath);
        assertNotNull(item);
        assertTrue(item instanceof Node);
        Node baseNode = (Node)item;

        Node resultSetNode_1 = baseNode.getNode(HippoNodeType.HIPPO_RESULTSET);
        assertNotNull(resultSetNode_1);

        Node resultSetNode_2 = (Node)session.getItem(basePath + "/" + HippoNodeType.HIPPO_RESULTSET);
        assertNotNull(resultSetNode_2);
    }

    @Test
    public void testGetItemFromNode() throws RepositoryException {
        commonStart();

        String basePath = "/test/navigation/xyz/x2/y1/z2";
        Item item = session.getItem(basePath);
        assertNotNull(item);
        assertTrue(item instanceof Node);
        Node baseNode = (Node)item;

        Node resultSetNode_1 = baseNode.getNode(HippoNodeType.HIPPO_RESULTSET);
        assertNotNull(resultSetNode_1);

        Node resultSetNode_2 = (Node)session.getItem(basePath + "/" + HippoNodeType.HIPPO_RESULTSET);
        assertNotNull(resultSetNode_2);
    }

    @Test
    public void testVirtualNodeHasNoJcrUUID() throws RepositoryException {
        commonStart();

        Node node = getSearchNode().getNode("x2").getNode("y1").getNode("z2");
        node = node.getNode(HippoNodeType.HIPPO_RESULTSET);

        // deliberate while loop to force that we have at least one child node to traverse
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            node = iter.nextNode();
            if (node != null) {
                assertFalse(node.hasProperty("jcr:uuid"));
                assertTrue(node.hasProperty("hippo:uuid"));
                assertTrue(node.hasProperty("hippo:paths"));
                assertNotNull(node.getProperty("hippo:uuid"));
                assertNotNull(node.getProperty("hippo:paths"));
                /* FIXME: enable these for checks for HREPTWO-283
                 *  assertFalse(node.isNodeType("mix:referenceable"));
                 */
            }
        }
    }

    @Test
    public void testAddingNodesOpenFacetSearch() throws RepositoryException {
        commonStart();

        Node node, child, searchNode = getSearchNode();
        //traverse(searchNode);

        assertFalse(searchNode.getNode("x1").hasNode("yy"));
        session.refresh(false);
        session.save();

        node = getDocsNode();
        child = node.addNode("test", "hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("x", "x1");
        child.setProperty("y", "yy");
        node.save();

        searchNode = getSearchNode();
        assertTrue(searchNode.getNode("x1").hasNode("yy"));
        assertTrue(searchNode.getNode("x1").getNode("yy").hasNode(HippoNodeType.HIPPO_RESULTSET));
        assertTrue(searchNode.getNode("x1").getNode("yy").getNode(HippoNodeType.HIPPO_RESULTSET).hasNode("test"));
        assertFalse(searchNode.getNode("x1").getNode("yy").getNode(HippoNodeType.HIPPO_RESULTSET).hasNode("test[2]"));

        node = getDocsNode();
        child = node.addNode("test", "hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("x", "x1");
        child.setProperty("y", "yy");
        session.save();
        session.refresh(false);

        searchNode = getSearchNode();
        assertTrue(searchNode.getNode("x1").hasNode("yy"));
        assertTrue(searchNode.getNode("x1").getNode("yy").hasNode(HippoNodeType.HIPPO_RESULTSET));
        assertTrue(searchNode.getNode("x1").getNode("yy").getNode(HippoNodeType.HIPPO_RESULTSET).hasNode("test"));
        assertTrue(searchNode.getNode("x1").getNode("yy").getNode(HippoNodeType.HIPPO_RESULTSET).hasNode("test[2]"));

        getDocsNode().getNode("test").setProperty("y","zz");
        session.save();
        session.refresh(false);

        searchNode = getSearchNode();
        assertTrue(searchNode.getNode("x1").hasNode("yy"));
        assertTrue(searchNode.getNode("x1").getNode("yy").hasNode(HippoNodeType.HIPPO_RESULTSET));
        assertTrue(searchNode.getNode("x1").getNode("yy").getNode(HippoNodeType.HIPPO_RESULTSET).hasNode("test"));
        assertFalse(searchNode.getNode("x1").getNode("yy").getNode(HippoNodeType.HIPPO_RESULTSET).hasNode("test[2]"));
        assertTrue(searchNode.getNode("x1").getNode("zz").hasNode(HippoNodeType.HIPPO_RESULTSET));

        commonEnd();
    }

    @Test
    public void testPerformance() throws RepositoryException {
        commonStart();
        Node searchNode = getSearchNode();
        searchNode.getNode("x2").getNode("y1").getNode("z2").getNode(HippoNodeType.HIPPO_RESULTSET).getProperty(HippoNodeType.HIPPO_COUNT).getLong();
        commonEnd();
    }
}
