/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.richtext.link;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.easymock.EasyMock;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.RichTextException;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.JcrNodeFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.NodeFactory;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RichTextLinkFactoryTest {

    private MockNode root;
    private Node htmlNode;
    private MockNode targetHandle;
    private NodeFactory nodeFactory;
    private MockNode links;

    @Before
    public void setUp() throws RepositoryException {
        root = MockNode.root();

        links = root.addNode("links", "nt:folder");

        targetHandle = root.addNode("target", "hippo:handle");
        targetHandle.addNode("target", "hippo:document");

        final Node sourceHandle = root.addNode("source", "hippo:handle");
        htmlNode = sourceHandle.addNode("source", "richtexttest:testdocument").addNode("richtexttest:html", "hippostd:html");
        htmlNode.setProperty("hippostd:content", "testing 1 2 3");

        nodeFactory = JcrNodeFactory.of(root);
    }

    @Test
    public void testCreateLinks() throws RichTextException, RepositoryException {
        final RichTextLinkFactoryImpl factory = getJcrRichTextLinkFactory();

        final Model<Node> linkModel1 = createLinkModel("link1");
        final RichTextLink link1 = factory.createLink(linkModel1);
        assertRichTextLink(link1, "link1");

        final Model<Node> linkModel2 = createLinkModel("link2");
        final RichTextLink link2 = factory.createLink(linkModel2);
        assertRichTextLink(link2, "link2");
    }

    @Test
    public void testCreateDuplicateLinks() throws Exception {
        final RichTextLinkFactoryImpl factory = getJcrRichTextLinkFactory();
        final Model<Node> linkModel = createLinkModel("link1");
        final RichTextLink link1 = factory.createLink(linkModel);
        final RichTextLink link2 = factory.createLink(linkModel);

        assertEquals(1, htmlNode.getNodes("link1").getSize());
        assertRichTextLink(link1, "link1");

        assertEquals(1, htmlNode.getNodes("link1_1").getSize());
        assertRichTextLink(link2, "link1", "link1_1");
    }

    @Test
    public void testThrowsIfTargetDoesNotExist() throws Exception {
        final RichTextLinkFactoryImpl factory = getJcrRichTextLinkFactory();
        try {
            factory.createLink(new Model<Node>() {
                @Override
                public Node get() {
                    return null;
                }

                @Override
                public void set(final Node value) {
                }
            });
            fail("Should throw an exception");
        } catch (final RichTextException e) {
            assertEquals("Link target node does not exist", e.getMessage());
        }
    }

    @Test
    public void testRichTextLinkReferencesTarget() throws RepositoryException, RichTextException {
        final RichTextLinkFactoryImpl factory = getJcrRichTextLinkFactory();
        final RichTextLink link = factory.createLink(nodeFactory.getNodeModelByNode(targetHandle));
        final Model<Node> target = link.getTargetModel();

        assertEquals("/target", target.get().getPath());
        assertEquals(root.getNode("target").getIdentifier(), link.getUuid());
    }

    @Test
    public void testIsValid() throws Exception {
        final RichTextLinkFactoryImpl factory = getJcrRichTextLinkFactory();

        assertFalse(factory.isValid(null));
        assertFalse(factory.isValid(Model.of(null)));

        final Node noReference = root.addNode("no-reference", "nt:unstructured");
        assertFalse(factory.isValid(nodeFactory.getNodeModelByNode(noReference)));

        final Node reference = root.addNode("reference", "nt:unstructured");
        reference.addMixin("mix:referenceable");
        assertTrue(factory.isValid(nodeFactory.getNodeModelByNode(reference)));

        final Node mockNode = EasyMock.createMock(Node.class);
        expect(mockNode.getIdentifier()).andReturn("broken-node-uuid");
        EasyMock.replay(mockNode);

        assertFalse(factory.isValid(nodeFactory.getNodeModelByNode(mockNode)));
        EasyMock.verify(mockNode);
    }

    @Test
    public void testLoadLink() throws Exception {
        final RichTextLinkFactoryImpl factory = getJcrRichTextLinkFactory();
        final RichTextLink linkCreated = factory.createLink(nodeFactory.getNodeModelByNode(targetHandle));

        final Node node = linkCreated.getTargetModel().get();
        final RichTextLink linkLoaded = factory.loadLink(node.getIdentifier());
        assertEquals("/target", linkLoaded.getTargetModel().get().getPath());
        assertEquals(root.getNode("target").getIdentifier(), linkLoaded.getUuid());
    }

    @Test
    public void testGetLinkUuids() throws Exception {
        final RichTextLinkFactoryImpl factory = getJcrRichTextLinkFactory();
        factory.createLink(nodeFactory.getNodeModelByNode(targetHandle));

        final MockNode second = root.addNode("second", "nt:unstructured");
        factory.createLink(nodeFactory.getNodeModelByNode(second));

        assertTrue(factory.hasLink(targetHandle.getIdentifier()));
        assertTrue(factory.hasLink(second.getIdentifier()));

    }

    private Model<Node> createLinkModel(final String linkName) throws RepositoryException {
        return nodeFactory.getNodeModelByNode(links.addNode(linkName, "hippo:handle"));
    }

    private void assertRichTextLink(final RichTextLink link, final String name) throws RepositoryException {
        assertRichTextLink(link, name, name);
    }

    private void assertRichTextLink(final RichTextLink link, final String name, final String facet) throws RepositoryException {
        assertTrue(htmlNode.hasNode(facet));
        assertEquals(HippoNodeType.NT_FACETSELECT, htmlNode.getNode(facet).getPrimaryNodeType().getName());

        final Model<Node> target = link.getTargetModel();
        assertEquals("/links/" + name, target.get().getPath());
        assertEquals(links.getNode(name).getIdentifier(), link.getUuid());

    }

    private RichTextLinkFactoryImpl getJcrRichTextLinkFactory() throws RepositoryException {
        return new RichTextLinkFactoryImpl(nodeFactory.getNodeModelByNode(htmlNode), nodeFactory);
    }
}
