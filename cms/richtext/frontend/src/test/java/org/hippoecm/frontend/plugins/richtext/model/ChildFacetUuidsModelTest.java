 /*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.richtext.model;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.easymock.EasyMock;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.ChildFacetUuidsModel;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ChildFacetUuidsModel}
 */
public class ChildFacetUuidsModelTest {

    private MockNode rootNode;
    private MockNode documentNode;
    private IModel<String> textModel;
    private IModel<Node> documentNodeModel;

    @Before
    public void setUp() throws RepositoryException {
        rootNode = MockNode.root();
        documentNode = new MockNode("document");
        rootNode.addNode(documentNode);

        documentNodeModel = EasyMock.createMock(IModel.class);
        expect(documentNodeModel.getObject()).andReturn(documentNode).anyTimes();
        replay(documentNodeModel);
    }

    private ChildFacetUuidsModel createModel(final String text) {
        textModel = new Model(text);
        final JcrRichTextLinkFactory linkFactory = new JcrRichTextLinkFactory(documentNodeModel);
        return new ChildFacetUuidsModel(textModel, documentNodeModel, linkFactory);
    }

    private void addChildFacetNode(final String name, final String uuid) throws RepositoryException {
        final Node child = documentNode.addNode(name, HippoNodeType.NT_FACETSELECT);
        child.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
    }

    private void assertNoChanges(String text) throws RepositoryException {
        assertEquals("Stored text should be returned without changes", text, createModel(text).getObject());

        final ChildFacetUuidsModel model = createModel("");
        final long childFacetCount = documentNode.getNodes().getSize();
        model.setObject(text);
        assertEquals("Text should be stored without changes", text, textModel.getObject());
        assertEquals("Number of child facet nodes should not have changed", childFacetCount, documentNode.getNodes().getSize());
    }

    private void assertSetTextUnchangedAndAllChildFacetsRemoved(String text) throws RepositoryException {
        final ChildFacetUuidsModel model = createModel("");
        model.setObject(text);
        assertEquals("all child facet nodes should have been removed", 0, documentNode.getNodes().getSize());
        assertEquals(text, textModel.getObject());
    }

    @Test
    public void nullTextDoesNotChange() throws RepositoryException {
        assertNoChanges(null);
    }

    @Test
    public void emptyTextDoesNotChange() throws RepositoryException {
        assertNoChanges("");
    }

    @Test
    public void getLinkChildNodeNameIsRewrittenToUuid() throws RepositoryException {
        addChildFacetNode("linked-node", "d1b804c0-cf19-451f-8c0f-184da74289e4");

        final ChildFacetUuidsModel model = createModel("<a href=\"linked-node\">link</a>");

        assertEquals("<a href=\"http://\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\">link</a>", model.getObject());
    }

    @Test
    public void getLinkWithEscapedNameIsRewrittenToUuid() throws RepositoryException {
        final String name = "A name that needs 'encoding'";
        final String linkTargetName = NodeNameCodec.encode(name, true);
        assertFalse(name.equals(linkTargetName));

        addChildFacetNode(linkTargetName, "d1b804c0-cf19-451f-8c0f-184da74289e4");

        final ChildFacetUuidsModel model = createModel("<a href=\"" + linkTargetName + "\">link</a>");

        assertEquals("<a href=\"http://\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\">link</a>", model.getObject());
    }

    @Test
    public void getImageChildNodeNameIsRewrittenToUuid() throws RepositoryException {
        addChildFacetNode("image.jpg", "d1b804c0-cf19-451f-8c0f-184da74289e4");

        final ChildFacetUuidsModel model = createModel("<img src=\"image.jpg/{_document}/hippogallery:thumbnail\"/>");

        assertEquals("<img src=\"image.jpg/{_document}/hippogallery:thumbnail\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\"/>", model.getObject());
    }

    @Test
    public void getMultipleLinkChildNodeNamesAreRewrittenToUuids() throws RepositoryException {
        addChildFacetNode("linked-node-1", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        addChildFacetNode("linked-node-2", "eb40e696-67db-4d5b-a09a-987e6c49543d");

        final ChildFacetUuidsModel model = createModel("Two links: <a href=\"linked-node-1\">one</a> and <a href=\"linked-node-2\">two</a>");

        assertEquals("Two links: <a href=\"http://\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\">one</a> and <a href=\"http://\" data-uuid=\"eb40e696-67db-4d5b-a09a-987e6c49543d\">two</a>", model.getObject());
    }

    @Test
    public void getMultipleImageChildNodeNamesAreRewrittenToUuids() throws RepositoryException {
        addChildFacetNode("foo.jpg", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        addChildFacetNode("bar.jpg", "eb40e696-67db-4d5b-a09a-987e6c49543d");

        final ChildFacetUuidsModel model = createModel("Two images: <img src=\"foo.jpg/{_document}/hippogallery:original\"/> and <img src=\"bar.jpg/{_document}/hippogallery:thumbnail\"/>");

        assertEquals("Two images: <img src=\"foo.jpg/{_document}/hippogallery:original\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\"/> and <img src=\"bar.jpg/{_document}/hippogallery:thumbnail\" data-uuid=\"eb40e696-67db-4d5b-a09a-987e6c49543d\"/>", model.getObject());
    }

    @Test
    public void getMissingImageChildNodeDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"no-such-image.jpg/{_document}/hippogallery:thumbnail\"/>");
    }

    @Test
    public void anchorDoesNotChange() throws RepositoryException {
        assertNoChanges("<a name=\"foo\">anchor</a>");
    }

    @Test
    public void relativeLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"somepage.html\">relative link</a>");
    }

    @Test
    public void relativeLinkWithPathDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"../somepage.html\">relative link with path</a>");
    }

    @Test
    public void relativeLinkWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"2*3=6.html\">Link to file with illegal JCR characters in its name</a>");
    }

    @Test
    public void externalHttpLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"http://www.example.com\">external link</a>");
    }

    @Test
    public void externalLinkWithPortDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"http://www.example.com:8080\">external link with port</a>");
    }

    @Test
    public void externalHttpOnlyLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"http://\">strange external link</a>");
    }

    @Test
    public void linkWithEmptyHrefDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"\">link with empty href</a>");
    }

    @Test
    public void linkWithEmptyHrefAndUuidDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"\" data-uuid=\"\">link with empty href and uuid</a>");
    }

    @Test
    public void externalFtpLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"ftp://www.example.com\">external FTP link</a>");
    }

    @Test
    public void externalFtpOnlyLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"ftp://\">strange external FTP link</a>");
    }

    @Test
    public void emptyLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a>strange empty link</a>");
    }

    @Test
    public void externalImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"http://www.example.com/foo.jpg\"/>");
    }

    @Test
    public void externalHttpOnlyImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"http://\"/>");
    }

    @Test
    public void externalRelativeImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"images/foo.jpg\"/>");
    }

    @Test
    public void externalRelativeWithPathImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"../images/foo.jpg\"/>");
    }

    @Test
    public void externalImageWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"2*3=6.png\">Link to image illegal JCR characters in its name</a>");
    }

    @Test
    public void externalRelativeImageWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("100% correct image: <img src=\"100%25+correct.png\"/>");
    }

    @Test
    public void setNewLinkUuidCreatesChildNodeAndReplacesUuid() throws RepositoryException {
        final Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");

        final ChildFacetUuidsModel model = createModel("");
        assertEquals(0, documentNode.getNodes().getSize());

        model.setObject("<a href=\"http://\" data-uuid=\"" + linkTarget.getIdentifier() + "\">link</a>");
        assertTrue("Text with new link UUID should create a child facet node", documentNode.hasNode("linked-node"));
        assertEquals("Text with new link UUID should create exactly one child facet node", 1, documentNode.getNodes().getSize());

        Node child = documentNode.getNode("linked-node");
        assertEquals(linkTarget.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<a href=\"linked-node\">link</a>", textModel.getObject());
    }

    @Test
    public void setNewImageUuidCreatesChildNodeAndRemovesUuid() throws RepositoryException {
        final Node linkedImage = rootNode.addNode("linked-image.jpg", "nt:unstructured");

        final ChildFacetUuidsModel model = createModel("");
        assertEquals(0, documentNode.getNodes().getSize());

        model.setObject("<img src=\"binaries/linked-image.jpg/linked-image.jpg/hippogallery-thumbnail\" data-uuid=\"" + linkedImage.getIdentifier() + "\"/>");
        assertTrue("Text with new image UUID should create a child facet node", documentNode.hasNode("linked-image.jpg"));
        assertEquals("Text with new image UUID should create exactly one child facet node", 1, documentNode.getNodes().getSize());

        Node child = documentNode.getNode("linked-image.jpg");
        assertEquals(linkedImage.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<img src=\"binaries/linked-image.jpg/linked-image.jpg/hippogallery-thumbnail\"/>", textModel.getObject());
    }

    @Test
    public void setExistingLinkUuidDoesNotCreateChildNode() throws RepositoryException {
        final Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        final ChildFacetUuidsModel model = createModel("");
        model.setObject("<a href=\"http://\" data-uuid=\"" + linkTarget.getIdentifier() + "\">link</a>");

        assertEquals("Text with existing link UUID should reuse existing child facet node", 1, documentNode.getNodes().getSize());

        Node child = documentNode.getNode("linked-node");
        assertEquals(linkTarget.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<a href=\"linked-node\">link</a>", textModel.getObject());
    }

    @Test
    public void setExistingImageUuidDoesNotCreateChildNode() throws RepositoryException {
        final Node linkedImage = rootNode.addNode("linked-image.jpg", "nt:unstructured");
        addChildFacetNode("linked-image.jpg", linkedImage.getIdentifier());

        final ChildFacetUuidsModel model = createModel("");
        model.setObject("<img src=\"binaries/linked-image.jpg/linked-image.jpg/hippogallery-thumbnail\" data-uuid=\"" + linkedImage.getIdentifier() + "\"/>");

        assertEquals("Text with existing image UUID should reuse existing child facet node", 1, documentNode.getNodes().getSize());

        Node child = documentNode.getNode("linked-image.jpg");
        assertEquals(linkedImage.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<img src=\"binaries/linked-image.jpg/linked-image.jpg/hippogallery-thumbnail\"/>", textModel.getObject());
    }

    @Test
    public void setExternalLinkWithUuidIgnoresUuid() throws RepositoryException {
        final Node document1 = rootNode.addNode("document1", "nt:unstructured");
        final ChildFacetUuidsModel model = createModel("");
        model.setObject("<a href=\"http://www.example.com\" data-uuid=\"" + document1.getIdentifier() + "\">external link</a>");
        assertEquals("No child facet nodes should have been created", 0, documentNode.getNodes().getSize());
        assertEquals("<a href=\"http://www.example.com\">external link</a>", textModel.getObject());
    }

    @Test
    public void setTextWithoutAnyLinksRemovesAllChildNodes() throws RepositoryException {
        final Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        assertSetTextUnchangedAndAllChildFacetsRemoved("Text without link");
    }

    @Test
    public void setEmptyTextRemovesAllChildNodes() throws RepositoryException {
        final Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        assertSetTextUnchangedAndAllChildFacetsRemoved("");
    }

    @Test
    public void setNullTextRemovesAllChildNodes() throws RepositoryException {
        final Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        assertSetTextUnchangedAndAllChildFacetsRemoved(null);
    }

    @Test
    public void setTextWithLinksRemovesUnusedChildNodes() throws RepositoryException {
        final Node document1 = rootNode.addNode("document1", "nt:unstructured");
        final Node document2 = rootNode.addNode("document2", "nt:unstructured");

        addChildFacetNode("document1", document1.getIdentifier());
        addChildFacetNode("document2", document2.getIdentifier());

        final ChildFacetUuidsModel model = createModel("");
        model.setObject("Text with only one link to <a href=\"http://\" data-uuid=\"" + document1.getIdentifier() + "\">document one</a>");

        final NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have only one facet child node", 1, documentNode.getNodes().getSize());

        final Node child = children.nextNode();
        assertEquals(document1.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("Text with only one link to <a href=\"" + child.getName() + "\">document one</a>", textModel.getObject());
    }

    @Test
    public void setTextWithImagesRemovesUnusedChildNodes() throws RepositoryException {
        final Node image1 = rootNode.addNode("image1.jpg", "nt:unstructured");
        final Node image2 = rootNode.addNode("image2.jpg", "nt:unstructured");

        addChildFacetNode("image1.jpg", image1.getIdentifier());
        addChildFacetNode("image2.jpg", image2.getIdentifier());

        final ChildFacetUuidsModel model = createModel("");
        model.setObject("Text with only one image: <img src=\"binaries/image1.jpg/{_document}/hippogallery:thumbnail\" data-uuid=\"" + image1.getIdentifier() + "\"/>");

        final NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have only one facet child node", 1, documentNode.getNodes().getSize());

        final Node child = children.nextNode();
        assertEquals(image1.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("Text with only one image: <img src=\"binaries/image1.jpg/{_document}/hippogallery:thumbnail\"/>", textModel.getObject());
    }

    @Test
    public void setTextWithLinksRemovesUnusedChildNodesWithAdditionalSuffix() throws RepositoryException {
        final Node document = rootNode.addNode("document1", "nt:unstructured");

        addChildFacetNode("document", document.getIdentifier());
        addChildFacetNode("document_1", document.getIdentifier());

        final ChildFacetUuidsModel model = createModel("");
        model.setObject("<a href=\"http://\" data-uuid=\"" + document.getIdentifier() + "\">document</a>");

        final NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have only one facet child node", 1, documentNode.getNodes().getSize());

        final Node child = children.nextNode();
        assertEquals(document.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<a href=\"" + child.getName() + "\">document</a>", textModel.getObject());
    }

    @Test
    public void setNewLinkUuidForTargetWithSameNameCreatesChildNodeWithDifferentName() throws RepositoryException {
        final Node document1 = rootNode.addNode("folder-one", "nt:unstructured").addNode("document", "nt:unstructured");
        final Node document2 = rootNode.addNode("folder-two", "nt:unstructured").addNode("document", "nt:unstructured");

        addChildFacetNode("document", document1.getIdentifier());

        final ChildFacetUuidsModel model = createModel("");
        model.setObject("<a href=\"http://\" data-uuid=\"" + document1.getIdentifier() + "\">document one</a>" +
                " and <a href=\"http://\" data-uuid=\"" + document2.getIdentifier() + "\">document two</a>");

        final NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have two facet child nodes", 2, documentNode.getNodes().getSize());

        final Node child1 = children.nextNode();
        assertEquals(document1.getIdentifier(), child1.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        final Node child2 = children.nextNode();
        assertEquals(document2.getIdentifier(), child2.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertFalse("Children should have different names", child1.getName().equals(child2.getName()));

        assertEquals("<a href=\"" + child1.getName() + "\">document one</a>" +
                " and <a href=\"" + child2.getName() + "\">document two</a>",
                textModel.getObject());
    }

    @Test
    public void setEmptyTextRemovesPreviouslyCreatedChildNodes() throws RepositoryException {
        final Node linked = rootNode.addNode("linked", "nt:unstructured");

        final ChildFacetUuidsModel model = createModel("");

        model.setObject("<a href=\"http://\" data-uuid=\"" + linked.getIdentifier() + "\">linked</a>");
        assertEquals("Child facet node should have been added", 1, documentNode.getNodes().getSize());

        model.setObject("");
        assertEquals("Child facet node should have been removed", 0, documentNode.getNodes().getSize());
    }

    @Test
    public void allStateIsDetached() {
        IModel<String> delegate = EasyMock.createMock(IModel.class);
        IModel<Node> nodeModel = EasyMock.createMock(IModel.class);
        IRichTextLinkFactory linkFactory = EasyMock.createMock(IRichTextLinkFactory.class);

        ChildFacetUuidsModel model = new ChildFacetUuidsModel(delegate, nodeModel, linkFactory);

        delegate.detach();
        expectLastCall();

        nodeModel.detach();
        expectLastCall();

        linkFactory.detach();
        expectLastCall();

        replay(delegate, nodeModel, linkFactory);

        model.detach();

        verify(delegate, nodeModel, linkFactory);
    }

}
