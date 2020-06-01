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
import static org.junit.Assert.assertFalse;
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
    String[] content2 = new String[] {
        "/test/selectnode",           "hippo:facetselect",
            "hippo:docbase",              "/test/content",
            "hippo:values",               null,
            "hippo:facets",               null,
            "hippo:modes",                null,
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(content1, session);
        session.save();
        session.refresh(false);
        build(content2, session);
        session.save();
        session.refresh(false);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
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
     * A virtual node in facetselet which IS a mirror of a hippo document returns
     * a node for getCanonicalNode()
     */
    @Test
    public void testFacetSelectNotNullCanonicalPath() throws RepositoryException {
        Node facetSelectNode = session.getRootNode().getNode("test/selectnode");
        assertTrue(((HippoNode) facetSelectNode.getNode("nodes").getNode("mynode")).getCanonicalNode() != null);
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

    /*
     * Assert that the canonical node of a mirrored hippo document has different ItemId
     */
    @Test
    public void testFacetSelectCanonicalNodeHasOtherIdTest() throws RepositoryException {
        Node facetSelectNode = session.getRootNode().getNode("test/selectnode");
        Node mirroredHippoDoc = facetSelectNode.getNode("nodes").getNode("mynode");
        ItemId mirroredId = unwrappedItemId(mirroredHippoDoc);
        Node canonical = ((HippoNode) mirroredHippoDoc).getCanonicalNode();
        ItemId canonicalId = unwrappedItemId(canonical);
        assertFalse(canonicalId.equals(mirroredId));
    }

    private ItemId unwrappedItemId(Node node) {
        Node impl = node;
        impl = org.hippoecm.repository.decorating.NodeDecorator.unwrap(impl);
        return ((NodeImpl)impl).getId();
    }

}
