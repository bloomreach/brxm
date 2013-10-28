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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.junit.Test;

public class FacetedNavigationChildNameTest extends FacetedNavigationAbstractTest {

    @Test
    public void testHREPTWO270Issue() throws RepositoryException {
        commonStart();

        final String simple = "testnode";
        final String encodeMe = "2..,!@#$%^&*()_-[]{}|\\:;'\".,/?testnode";

        Node node = session.getRootNode().getNode("test/documents");
        Node docNode = node.addNode(simple, "hippo:testdocument");
        docNode.addMixin("mix:versionable");
        docNode.setProperty("x", "success");
        docNode.setProperty("y", encodeMe);
        session.save();
        session.refresh(false);

        Node facetNode = getSearchNode().getNode("success");
        assertNotNull(facetNode);
        facetNode.hashCode(); // This earlier caused a NullPointerException, see HREPTWO-269
        assertEquals("success", facetNode.getName());

        Node nodeFromFacet = facetNode.getNode(HippoNodeType.HIPPO_RESULTSET).getNodes().nextNode();
        String path = "success/" + NodeNameCodec.encode(encodeMe, true);
        facetNode = getSearchNode().getNode(path);
        assertEquals(NodeNameCodec.encode(encodeMe, true), facetNode.getName());
        nodeFromFacet = facetNode.getNode(HippoNodeType.HIPPO_RESULTSET).getNodes().nextNode();
        docNode = session.getRootNode().getNode("test/documents").getNode(simple);
        assertTrue(nodeFromFacet.hasProperty("x"));
        assertTrue(nodeFromFacet.hasProperty("y"));
        assertEquals(docNode.getProperty("x").getString(), nodeFromFacet.getProperty("x").getString());
        assertEquals(docNode.getProperty("y").getString(), nodeFromFacet.getProperty("y").getString());

        commonEnd();

    }
}
