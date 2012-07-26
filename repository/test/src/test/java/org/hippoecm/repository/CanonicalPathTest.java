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
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class CanonicalPathTest extends TestCase {

    String[] content1 = new String[] {
        "/test",                      "nt:unstructured",
        "/test/content",              "nt:unstructured",
        "jcr:mixinTypes",             "mix:referenceable",
        "/test/content/nodes",        "nt:unstructured",
        "/test/content/nodes/mynode", "hippo:testdocument",
        "jcr:mixinTypes",             "hippo:harddocument",
        "x",                          "foo"
    };
    String[] content2 = new String[] {
        "/test/selectnode",           "hippo:facetselect",
        "hippo:docbase",              "/test/content",
        "hippo:values",               null,
        "hippo:facets",               null,
        "hippo:modes",                null,
        "/test/searchnode",           "hippo:facetsearch",
        "hippo:docbase",              "/test/content",
        "hippo:queryname",            "xyz",
        "hippo:facets",               "x"
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(session, content1);
        session.save();
        session.refresh(false);
        build(session, content2);
        session.save();
        session.refresh(false);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Basic test on the root node: see HREPTWO-2342
     */
    @Test
    public void testRootNode() throws RepositoryException {
        Node root = session.getRootNode();
        assertTrue(root instanceof HippoNode);
        Node node = ((HippoNode) root).getCanonicalNode();
        assertNotNull(node);
        assertTrue(node.isSame(root));
    }
    
    /**
     * A virtual node in facetsearch which is not a mirror of a hippo document returns null
     * for getCanonicalNode()
     */
    @Test
    public void testFacetSearchNullCanonicalPath() throws RepositoryException {
        Node facetSearchNode = session.getRootNode().getNode("test/searchnode");
        assertTrue(((HippoNode)facetSearchNode.getNode("foo")).getCanonicalNode() == null);
    }

    /**
     * A virtual node in facetsearch which IS a mirror of a hippo document returns
     * a node for getCanonicalNode()
     */
    @Test
    public void testFacetSearchNotNullCanonicalPath() throws RepositoryException {
        Node facetSearchNode = session.getRootNode().getNode("test/searchnode");
        assertTrue(((HippoNode)facetSearchNode.getNode("foo").getNode(HippoNodeType.HIPPO_RESULTSET).getNode("mynode")).getCanonicalNode() != null);
    }

    /**
     * Assert that the canonical node of a mirrored hippo document is not the same
     */
    @Test
    public void testFacetSearchCanonicalNodeIsNotSameTest() throws RepositoryException {
        Node facetSearchNode = session.getRootNode().getNode("test/searchnode");
        Node mirroredHippoDoc = facetSearchNode.getNode("foo").getNode(HippoNodeType.HIPPO_RESULTSET).getNode("mynode");
        Node canonical = ((HippoNode)mirroredHippoDoc).getCanonicalNode();
        assertFalse(canonical.isSame(mirroredHippoDoc));
    }

    /**
     * A virtual node in facetselet which IS a mirror of a hippo document returns
     * a node for getCanonicalNode()
     */
    @Test
    public void testFacetSelectNotNullCanonicalPath() throws RepositoryException {
        Node facetSelectNode = session.getRootNode().getNode("test/selectnode");
        assertTrue(((HippoNode)facetSelectNode.getNode("nodes").getNode("mynode")).getCanonicalNode() != null);
    }

    /**
     * Assert that the canonical node of a mirrored hippo document is not the same
     */
    @Test
    public void testFacetSelectCanonicalNodeIsNotSameTest() throws RepositoryException {
        Node facetSelectNode = session.getRootNode().getNode("test/selectnode");
        Node mirroredHippoDoc = facetSelectNode.getNode("nodes").getNode("mynode");
        Node canonical = ((HippoNode)mirroredHippoDoc).getCanonicalNode();
        assertFalse(canonical.isSame(mirroredHippoDoc));
    }

    /**
     * Asserts that also non referenceable nodes have a canonical node when applicable
     */
    @Test
    public void testNonReferenceableHaveCanonical() throws RepositoryException {
        Node node = traverse(session, "/test/selectnode/nodes");
        assertNotNull(node);
        assertTrue(node instanceof HippoNode);
        Node canonical = ((HippoNode)node).getCanonicalNode();
        assertNotNull(canonical);
        assertEquals("/test/content/nodes", canonical.getPath());
    }

    /**
     * Asserts that nodes that have been moved have a canonical node
     */
    @Test
    public void testMovedNodeHasCanonical() throws RepositoryException {
        Node node = traverse(session, "/test/content/nodes");
        node.getSession().move(node.getPath(), node.getParent().getPath() + "/newnodes");
        assertNotNull(node);
        assertTrue(node instanceof HippoNode);
        Node canonical = ((HippoNode)node).getCanonicalNode();
        assertNotNull(canonical);
        assertEquals("/test/content/newnodes", canonical.getPath());
        node.getSession().save();
        canonical = ((HippoNode)node).getCanonicalNode();
        assertNotNull(canonical);
        assertEquals("/test/content/newnodes", canonical.getPath());
    }
}
