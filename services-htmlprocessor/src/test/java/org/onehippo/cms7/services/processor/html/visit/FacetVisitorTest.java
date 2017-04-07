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
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.richtext.jcr.JcrNodeFactory;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;

public class FacetVisitorTest {

    private MockNode root;
    private MockNode document;
    private Model<Node> documentModel;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();
        final JcrNodeFactory factory = new JcrNodeFactory() {
            @Override
            protected Session getSession() throws RepositoryException {
                return root.getSession();
            }
        };

        document = root.addNode("document", "hippo:document");
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
            public void visitBeforeRead(final Tag parent, final Tag tag) throws RepositoryException {}
        };
        visitor.visitBeforeWrite(null, null);

        assertEquals(0, document.getNodes().getSize());
    }

    private void addChildFacetNode(final String name, final String uuid) throws RepositoryException {
        final Node child = document.addNode(name, HippoNodeType.NT_FACETSELECT);
        child.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
    }
}
