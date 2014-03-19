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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.hippoecm.repository.api.HippoNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CanonicalPathTest extends RepositoryTestCase {

    String[] content1 = new String[] {
        "/test",                      "nt:unstructured",
            "/test/content",              "nt:unstructured",
                "jcr:mixinTypes",             "mix:referenceable",
                "/test/content/nodes",        "nt:unstructured",
                    "/test/content/nodes/mynode", "hippo:testdocument",
                        "jcr:mixinTypes",             "mix:versionable",
                        "x",                          "foo"
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(content1, session);
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

    /*
     * Assert that the canonical node of real hippo document has same ItemId
     */
    @Test
    public void testPhysicalCanonicalNodeHasSameIdTest() throws RepositoryException{
        Node physicalHippoDoc = session.getRootNode().getNode("test").getNode("content");
        ItemId physicalId = unwrappedItemId(physicalHippoDoc);
        Node canonical = ((HippoNode)physicalHippoDoc).getCanonicalNode();
        ItemId canonicalId = unwrappedItemId(canonical);
        assertTrue(canonicalId.equals(physicalId));
    }

    private ItemId unwrappedItemId(Node node) {
        Node impl = node;
        impl = org.hippoecm.repository.decorating.NodeDecorator.unwrap(impl);
        return ((NodeImpl)impl).getId();
    }

}
