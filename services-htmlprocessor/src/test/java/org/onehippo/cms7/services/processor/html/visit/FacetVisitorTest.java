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
package org.onehippo.cms7.services.processor.html.visit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.richtext.TestUtil;
import org.onehippo.cms7.services.processor.richtext.jcr.JcrNodeFactory;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class FacetVisitorTest {

    private MockNode root;
    private MockNode document;
    private Model<Node> documentModel;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();
        document = root.addNode("document", "hippo:document");

        final JcrNodeFactory factory = JcrNodeFactory.of(root);
        documentModel = factory.getNodeModelByNode(document);
    }

    @Test
    public void testWriteCleansExistingFacets() throws Exception {
        final Node facet1 = root.addNode("facet1", "nt:unstructured");
        final Node facet2 = root.addNode("facet2", "nt:unstructured");

        addChildFacetNode("facet1", facet1.getIdentifier());
        addChildFacetNode("facet2", facet2.getIdentifier());

        final FacetVisitor visitor = new FacetVisitor(documentModel) {
            @Override
            public void onRead(final Tag parent, final Tag tag) throws RepositoryException {}
        };
        visitor.onWrite(null, null);

        assertEquals(0, document.getNodes().getSize());
    }

    @Test
    public void testNodeModelIsReleased() throws Exception {
        final Node node = root.addNode("node1", "nt:unstructured");
        final Model<Node> mockNodeModel = EasyMock.createMock(Model.class);
        mockNodeModel.release();
        expectLastCall();
        expect(mockNodeModel.get()).andReturn(node);
        replay(mockNodeModel);

        final FacetVisitor visitor = new FacetVisitor(mockNodeModel) {
            @Override
            public void onRead(final Tag parent, final Tag tag) throws RepositoryException {}
        };
        visitor.onWrite(null, null);
        visitor.release();
        verify(mockNodeModel);
    }

    private void addChildFacetNode(final String name, final String uuid) throws RepositoryException {
        TestUtil.addChildFacetNode(document, name, uuid);
    }
}
