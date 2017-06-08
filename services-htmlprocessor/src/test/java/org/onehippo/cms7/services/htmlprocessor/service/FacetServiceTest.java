/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.service;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.TestUtil;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.JcrNodeFactory;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FacetServiceTest {

    private MockNode document;
    private Model<Node> documentModel;
    private MockNode doc1;
    private MockNode doc2;

    @Before
    public void setUp() throws Exception {
        final MockNode root = MockNode.root();
        document = root.addNode("document", "hippo:document");

        final JcrNodeFactory factory = JcrNodeFactory.of(root);
        documentModel = factory.getNodeModelByNode(document);

        doc1 = root.addNode("doc1", "nt:unstructured");
        doc2 = root.addNode("doc2", "nt:unstructured");
    }

    @Test
    public void testZeroFacets() throws Exception {
        final FacetService service = createService();
        service.removeUnmarkedFacets();
    }

    @Test
    public void testGetFacetIdByName() throws Exception {
        FacetService service = createService();
        assertNull(service.getFacetId("facet0"));

        addChildFacetNode("facet1", doc1.getIdentifier());
        service = createService();
        assertEquals(service.getFacetId("facet1"), doc1.getIdentifier());

        addChildFacetNode("facet2", doc2.getIdentifier());
        service = createService();
        assertEquals(service.getFacetId("facet1"), doc1.getIdentifier());
        assertEquals(service.getFacetId("facet2"), doc2.getIdentifier());
    }

    @Test
    public void testGetFacetNameById() throws Exception {
        FacetService service = createService();
        assertNull(service.getFacetName("facet0"));

        service = createService();
        assertNull(service.getFacetId("facet0"));

        addChildFacetNode("facet1", doc1.getIdentifier());
        service = createService();
        assertEquals("facet1", service.getFacetName(doc1.getIdentifier()));

        addChildFacetNode("facet2", doc2.getIdentifier());
        service = createService();
        assertEquals("facet1", service.getFacetName(doc1.getIdentifier()));
        assertEquals("facet2", service.getFacetName(doc2.getIdentifier()));
    }

    @Test
    public void testUnmarkedFacetsAreRemoved() throws Exception {
        addChildFacetNode("facet1", doc1.getIdentifier());

        FacetService service = createService();
        assertTrue(document.hasNode("facet1"));

        service.removeUnmarkedFacets();
        assertFalse(document.hasNode("facet1"));

        addChildFacetNode("facet1", doc1.getIdentifier());
        addChildFacetNode("facet2", doc2.getIdentifier());

        service = createService();
        service.markVisited("facet1");
        service.removeUnmarkedFacets();

        assertTrue(document.hasNode("facet1"));
        assertFalse(document.hasNode("facet2"));
    }

    @Test
    public void testFacetsMapIsCleared() throws Exception {
        addChildFacetNode("facet1", doc1.getIdentifier());
        addChildFacetNode("facet2", doc2.getIdentifier());

        final FacetService service = createService();
        assertNotNull(service.getFacetId("facet1"));
        assertNotNull(service.getFacetId("facet2"));

        service.markVisited("facet1");
        service.removeUnmarkedFacets();

        assertNull(service.getFacetId("facet1"));
        assertNull(service.getFacetId("facet2"));
    }

    @Test
    public void testExistingFacetsAreNotRecreated() throws Exception {
        addChildFacetNode("facet1", doc1.getIdentifier());
        final String facet1NodeId = document.getNode("facet1").getIdentifier();

        final FacetService service = createService();
        service.markVisited("facet1");
        service.removeUnmarkedFacets();

        assertEquals(facet1NodeId, document.getNode("facet1").getIdentifier());
    }

    private FacetService createService() {
        return new FacetService(document);
    }

    private void addChildFacetNode(final String name, final String uuid) throws RepositoryException {
        TestUtil.addChildFacetNode(document, name, uuid);
    }

}
