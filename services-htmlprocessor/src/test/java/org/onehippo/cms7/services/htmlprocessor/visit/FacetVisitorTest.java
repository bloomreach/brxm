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
package org.onehippo.cms7.services.htmlprocessor.visit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.Tag;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.TestUtil;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.JcrNodeFactory;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FacetVisitorTest {

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
    public void testGetFacetIdByName() throws Exception {
        final FacetVisitor visitor = createVisitor(documentModel);
        assertNull(visitor.getFacetId("facet0"));

        visitor.before();
        assertNull(visitor.getFacetId("facet0"));

        addChildFacetNode("facet1", doc1.getIdentifier());
        visitor.before();
        assertEquals(visitor.getFacetId("facet1"), doc1.getIdentifier());

        addChildFacetNode("facet2", doc2.getIdentifier());
        visitor.before();
        assertEquals(visitor.getFacetId("facet1"), doc1.getIdentifier());
        assertEquals(visitor.getFacetId("facet2"), doc2.getIdentifier());
    }

    @Test
    public void testGetFacetNameById() throws Exception {
        final FacetVisitor visitor = createVisitor(documentModel);
        assertNull(visitor.getFacetName("facet0"));

        visitor.before();
        assertNull(visitor.getFacetId("facet0"));

        addChildFacetNode("facet1", doc1.getIdentifier());
        visitor.before();
        assertEquals("facet1", visitor.getFacetName(doc1.getIdentifier()));

        addChildFacetNode("facet2", doc2.getIdentifier());
        visitor.before();
        assertEquals("facet1", visitor.getFacetName(doc1.getIdentifier()));
        assertEquals("facet2", visitor.getFacetName(doc2.getIdentifier()));
    }

    @Test
    public void testZeroFacets() throws Exception {
        final FacetVisitor visitor = createVisitor(documentModel);
        visitor.before();
        visitor.onRead(null, null);
        visitor.onWrite(null, null);
        visitor.after();
    }

    @Test
    public void testUnmarkedFacetsAreRemoved() throws Exception {
        addChildFacetNode("facet1", doc1.getIdentifier());

        final FacetVisitor visitor = createVisitor(documentModel);
        visitor.before();
        assertTrue(document.hasNode("facet1"));

        visitor.after();
        assertFalse(document.hasNode("facet1"));

        addChildFacetNode("facet1", doc1.getIdentifier());
        addChildFacetNode("facet2", doc2.getIdentifier());

        visitor.before();
        visitor.markVisited("facet1");
        visitor.after();

        assertTrue(document.hasNode("facet1"));
        assertFalse(document.hasNode("facet2"));
    }

    @Test
    public void testFacetsMapIsCleared() throws Exception {
        addChildFacetNode("facet1", doc1.getIdentifier());
        addChildFacetNode("facet2", doc2.getIdentifier());

        final FacetVisitor visitor = createVisitor(documentModel);
        visitor.before();

        assertNotNull(visitor.getFacetId("facet1"));
        assertNotNull(visitor.getFacetId("facet2"));

        visitor.markVisited("facet1");
        visitor.after();

        assertNull(visitor.getFacetId("facet1"));
        assertNull(visitor.getFacetId("facet2"));
    }

    @Test
    public void testExistingFacetsAreNotRecreated() throws Exception {
        addChildFacetNode("facet1", doc1.getIdentifier());
        final String facet1NodeId = document.getNode("facet1").getIdentifier();

        final FacetVisitor visitor = createVisitor(documentModel);
        visitor.before();
        visitor.markVisited("facet1");
        visitor.after();

        assertEquals(facet1NodeId, document.getNode("facet1").getIdentifier());
    }


    @Test
    public void testNodeModelIsReleased() throws Exception {
        final Model<Node> mockNodeModel = EasyMock.createMock(Model.class);
        mockNodeModel.release();
        expectLastCall();
        replay(mockNodeModel);

        final FacetVisitor visitor = createVisitor(mockNodeModel);
        visitor.release();
        verify(mockNodeModel);
    }

    private void addChildFacetNode(final String name, final String uuid) throws RepositoryException {
        TestUtil.addChildFacetNode(document, name, uuid);
    }


    private FacetVisitor createVisitor(final Model<Node> model) {
        return new FacetVisitor(model) {
            @Override
            public void onRead(final Tag parent, final Tag tag) throws RepositoryException {
            }

            @Override
            public void onWrite(final Tag parent, final Tag tag) throws RepositoryException {
            }
        };
    }
}
