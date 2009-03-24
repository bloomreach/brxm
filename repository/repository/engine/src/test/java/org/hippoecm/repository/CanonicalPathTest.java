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

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeImpl;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class CanonicalPathTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode("test");
    }

    private Node commonFacetSelectSetup() throws RepositoryException {
        createContent();
        Node selectnode = session.getRootNode().getNode("test").addNode("selectnode", HippoNodeType.NT_FACETSELECT);
        selectnode.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/content").getUUID());
        selectnode.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { });
        selectnode.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { });
        selectnode.setProperty(HippoNodeType.HIPPO_MODES, new String[] { });
        session.save();
        session.refresh(false);
        return session.getRootNode().getNode("test").getNode("selectnode");
    }

    private Node commonFacetSearchSetup() throws RepositoryException {
        createContent();
        Node searchnode = session.getRootNode().getNode("test").addNode("searchnode", HippoNodeType.NT_FACETSEARCH);
        searchnode.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/content").getUUID());
        searchnode.setProperty(HippoNodeType.HIPPO_QUERYNAME, "xyz");
        searchnode.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "x"});
        session.save();
        session.refresh(false);
        return session.getRootNode().getNode("test").getNode("searchnode");
    }

    private void createContent() throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException, NoSuchNodeTypeException, ValueFormatException {
        Node node = session.getRootNode().getNode("test").addNode("content","nt:unstructured");
        node.addMixin("mix:referenceable");
        Node mynode = node.addNode("nodes").addNode("mynode","hippo:testdocument");
        mynode.addMixin("hippo:harddocument");
        mynode.setProperty("x", "foo");
        session.save();
    }

    /*
     * Basic test on the root node: see HREPTWO-2342
     */
    @Test public void testRootNode() throws RepositoryException {
        ((HippoNode) session.getRootNode()).getCanonicalNode();
    }
    
    /*
     * A virtual node in facetsearch which is not a mirror of a hippo document returns null
     * for getCanonicalNode()
     */
    @Test
    public void testFacetSearchNullCanonicalPath() throws RepositoryException{
        Node facetSearchNode = commonFacetSearchSetup();
        assertTrue(((HippoNode)facetSearchNode.getNode("foo")).getCanonicalNode() == null);
    }

    /*
     * A virtual node in facetsearch which IS a mirror of a hippo document returns
     * a node for getCanonicalNode()
     */
    @Test
    public void testFacetSearchNotNullCanonicalPath() throws RepositoryException{
        Node facetSearchNode = commonFacetSearchSetup();
        assertTrue(((HippoNode)facetSearchNode.getNode("foo").getNode(HippoNodeType.HIPPO_RESULTSET).getNode("mynode")).getCanonicalNode() != null);
    }

    /*
     * Assert that the canonical node of a mirrored hippo document is not the same
     */
    @Test
    public void testFacetSearchCanonicalNodeIsNotSameTest() throws RepositoryException{
        Node facetSearchNode = commonFacetSearchSetup();
        Node mirroredHippoDoc = facetSearchNode.getNode("foo").getNode(HippoNodeType.HIPPO_RESULTSET).getNode("mynode");
        Node canonical = ((HippoNode)mirroredHippoDoc).getCanonicalNode();
        assertFalse(canonical.isSame(mirroredHippoDoc));
    }

    /*
     * A virtual node in facetselet which is not a mirror of a hippo document returns null
     * for getCanonicalNode()
     */
    @Test
    public void testFacetSelectNullCanonicalPath() throws RepositoryException{
        Node facetSelectNode = commonFacetSelectSetup();
        assertTrue(((HippoNode)facetSelectNode.getNode("nodes")).getCanonicalNode() == null);
    }

    /*
     * A virtual node in facetselet which IS a mirror of a hippo document returns
     * a node for getCanonicalNode()
     */
    @Test
    public void testFacetSelectNotNullCanonicalPath() throws RepositoryException{
        Node facetSelectNode = commonFacetSelectSetup();
        assertTrue(((HippoNode)facetSelectNode.getNode("nodes").getNode("mynode")).getCanonicalNode() != null);
    }

    /*
     * Assert that the canonical node of a mirrored hippo document is not the same
     */
    @Test
    public void testFacetSelectCanonicalNodeIsNotSameTest() throws RepositoryException{
        Node facetSelectNode = commonFacetSelectSetup();
        Node mirroredHippoDoc = facetSelectNode.getNode("nodes").getNode("mynode");
        Node canonical = ((HippoNode)mirroredHippoDoc).getCanonicalNode();
        assertFalse(canonical.isSame(mirroredHippoDoc));
    }
}
