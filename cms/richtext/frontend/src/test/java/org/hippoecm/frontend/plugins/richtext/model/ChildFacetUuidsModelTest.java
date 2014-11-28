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

import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.easymock.EasyMock;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextImage;
import org.hippoecm.frontend.plugins.richtext.jcr.ChildFacetUuidsModel;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.RichTextImageURLProvider;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.eq;
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
    private IModel<Node> documentNodeModel;
    private IModel<String> textModel;
    private ChildFacetUuidsModel model;

    @Before
    public void setUp() throws RepositoryException, RichTextException {
        rootNode = MockNode.root();

        documentNode = new MockNode("document");
        rootNode.addNode(documentNode);

        documentNodeModel = EasyMock.createMock(IModel.class);
        expect(documentNodeModel.getObject()).andReturn(documentNode).anyTimes();

        replay(documentNodeModel);

        JcrRichTextLinkFactory linkFactory = new JcrRichTextLinkFactory(documentNodeModel);
        PrefixingImageUrlProvider imageUrlProvider = new PrefixingImageUrlProvider("/binaries");

        textModel = new Model("");
        model = new ChildFacetUuidsModel(textModel, documentNodeModel, linkFactory, imageUrlProvider);
    }

    private void addChildFacetNode(String name, String uuid) throws RepositoryException {
        Node child = documentNode.addNode(name, HippoNodeType.NT_FACETSELECT);
        child.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
    }

    private void assertNoChanges(String text) throws RepositoryException {
        model.setObject(text);
        assertEquals("Stored text should be returned without changes", text, model.getObject());

        long childFacetCount = documentNode.getNodes().getSize();
        model.setObject(text);
        assertEquals("Text should be stored without changes", text, textModel.getObject());
        assertEquals("Number of child facet nodes should not have changed", childFacetCount, documentNode.getNodes().getSize());
    }

    private void assertSetTextUnchangedAndAllChildFacetsRemoved(String text) throws RepositoryException {
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
        textModel.setObject("<a href=\"linked-node\">link</a>");
        assertEquals("<a href=\"http://\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\">link</a>", model.getObject());
    }

    @Test
    public void getLinkWithEscapedNameIsRewrittenToUuid() throws RepositoryException {
        String name = "A name that needs 'encoding'";
        String linkTargetName = NodeNameCodec.encode(name, true);
        assertFalse(name.equals(linkTargetName));

        addChildFacetNode(linkTargetName, "d1b804c0-cf19-451f-8c0f-184da74289e4");

        textModel.setObject("<a href=\"" + linkTargetName + "\">link</a>");

        assertEquals("<a href=\"http://\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\">link</a>", model.getObject());
    }

    @Test
    public void getImageChildNodeNameIsRewrittenToUuid() throws RepositoryException {
        addChildFacetNode("image.jpg", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        textModel.setObject("<img src=\"image.jpg/{_document}/hippogallery:thumbnail\" />");
        assertEquals("<img src=\"/binaries/image.jpg/{_document}/hippogallery:thumbnail\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\" data-type=\"hippogallery:thumbnail\" />", model.getObject());
    }

    @Test
    public void getMultipleLinkChildNodeNamesAreRewrittenToUuids() throws RepositoryException {
        addChildFacetNode("linked-node-1", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        addChildFacetNode("linked-node-2", "eb40e696-67db-4d5b-a09a-987e6c49543d");

        textModel.setObject("Two links: <a href=\"linked-node-1\">one</a> and <a href=\"linked-node-2\">two</a>");

        assertEquals("Two links: <a href=\"http://\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\">one</a> and <a href=\"http://\" data-uuid=\"eb40e696-67db-4d5b-a09a-987e6c49543d\">two</a>", model.getObject());
    }

    @Test
    public void getMultipleImageChildNodeNamesAreRewrittenToUuids() throws RepositoryException {
        addChildFacetNode("foo.jpg", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        addChildFacetNode("bar.jpg", "eb40e696-67db-4d5b-a09a-987e6c49543d");
        textModel.setObject("Two images: <img src=\"foo.jpg/{_document}/hippogallery:original\" /> and <img src=\"bar.jpg/{_document}/hippogallery:original\" />");
        assertEquals("Two images: <img src=\"/binaries/foo.jpg/{_document}/hippogallery:original\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\" data-type=\"hippogallery:original\" /> and <img src=\"/binaries/bar.jpg/{_document}/hippogallery:original\" data-uuid=\"eb40e696-67db-4d5b-a09a-987e6c49543d\" data-type=\"hippogallery:original\" />", model.getObject());
    }

    @Test
    public void getMissingImageChildNodeDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"no-such-image.jpg/{_document}/hippogallery:original\" />");
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
        assertNoChanges("<img src=\"http://www.example.com/foo.jpg\" />");
    }

    @Test
    public void externalHttpOnlyImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"http://\" />");
    }

    @Test
    public void externalRelativeImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"images/foo.jpg\" />");
    }

    @Test
    public void externalRelativeWithPathImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"../images/foo.jpg\" />");
    }

    @Test
    public void externalImageWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("<a><img src=\"2*3=6.png\" />Link to image illegal JCR characters in its name</a>");
    }

    @Test
    public void externalRelativeImageWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("100% correct image: <img src=\"100%25+correct.png\" />");
    }

    @Test
    public void setNewLinkUuidCreatesChildNodeAndReplacesUuid() throws RepositoryException {
        Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");

        model.setObject("");
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
        Node linkedImage = rootNode.addNode("linked-image.jpg", "nt:unstructured");

        model.setObject("");
        assertEquals(0, documentNode.getNodes().getSize());

        model.setObject("<img src=\"/binaries/linked-image.jpg/linked-image.jpg/hippogallery:thumbnail\" data-uuid=\"" + linkedImage.getIdentifier() + "\" data-type=\"hippogallery:thumbnail\" />");
        assertTrue("Text with new image UUID should create a child facet node", documentNode.hasNode("linked-image.jpg"));
        assertEquals("Text with new image UUID should create exactly one child facet node", 1, documentNode.getNodes().getSize());

        Node child = documentNode.getNode("linked-image.jpg");
        assertEquals(linkedImage.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<img src=\"linked-image.jpg/{_document}/hippogallery:thumbnail\" />", textModel.getObject());
    }

    @Test
    public void setExistingLinkUuidDoesNotCreateChildNode() throws RepositoryException {
        Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        model.setObject("");
        model.setObject("<a href=\"http://\" data-uuid=\"" + linkTarget.getIdentifier() + "\">link</a>");

        assertEquals("Text with existing link UUID should reuse existing child facet node", 1, documentNode.getNodes().getSize());

        Node child = documentNode.getNode("linked-node");
        assertEquals(linkTarget.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<a href=\"linked-node\">link</a>", textModel.getObject());
    }

    @Test
    public void setExistingImageUuidDoesNotCreateChildNode() throws RepositoryException {
        Node linkedImage = rootNode.addNode("linked-image.jpg", "nt:unstructured");
        addChildFacetNode("linked-image.jpg", linkedImage.getIdentifier());

        model.setObject("<img src=\"/binaries/linked-image.jpg/linked-image.jpg/hippogallery:thumbnail\" data-uuid=\"" + linkedImage.getIdentifier() + "\" data-type=\"hippogallery:thumbnail\" />");

        assertEquals("Text with existing image UUID should reuse existing child facet node", 1, documentNode.getNodes().getSize());

        Node child = documentNode.getNode("linked-image.jpg");
        assertEquals(linkedImage.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<img src=\"linked-image.jpg/{_document}/hippogallery:thumbnail\" />", textModel.getObject());
    }

    @Test
    public void setExternalLinkWithUuidIgnoresUuid() throws RepositoryException {
        Node document1 = rootNode.addNode("document1", "nt:unstructured");
        model.setObject("");
        model.setObject("<a href=\"http://www.example.com\" data-uuid=\"" + document1.getIdentifier() + "\">external link</a>");
        assertEquals("No child facet nodes should have been created", 0, documentNode.getNodes().getSize());
        assertEquals("<a href=\"http://www.example.com\">external link</a>", textModel.getObject());
    }

    @Test
    public void setTextWithoutAnyLinksRemovesAllChildNodes() throws RepositoryException {
        Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        assertSetTextUnchangedAndAllChildFacetsRemoved("Text without link");
    }

    @Test
    public void setEmptyTextRemovesAllChildNodes() throws RepositoryException {
        Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        assertSetTextUnchangedAndAllChildFacetsRemoved("");
    }

    @Test
    public void setNullTextRemovesAllChildNodes() throws RepositoryException {
        Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        assertSetTextUnchangedAndAllChildFacetsRemoved(null);
    }

    @Test
    public void setTextWithLinksRemovesUnusedChildNodes() throws RepositoryException {
        Node document1 = rootNode.addNode("document1", "nt:unstructured");
        Node document2 = rootNode.addNode("document2", "nt:unstructured");

        addChildFacetNode("document1", document1.getIdentifier());
        addChildFacetNode("document2", document2.getIdentifier());

        model.setObject("");
        model.setObject("Text with only one link to <a href=\"http://\" data-uuid=\"" + document1.getIdentifier() + "\">document one</a>");

        NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have only one facet child node", 1, documentNode.getNodes().getSize());

        Node child = children.nextNode();
        assertEquals(document1.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("Text with only one link to <a href=\"" + child.getName() + "\">document one</a>", textModel.getObject());
    }

    @Test
    public void setTextWithImagesRemovesUnusedChildNodes() throws RepositoryException {
        Node image1 = rootNode.addNode("image1.jpg", "nt:unstructured");
        Node image2 = rootNode.addNode("image2.jpg", "nt:unstructured");

        addChildFacetNode("image1.jpg", image1.getIdentifier());
        addChildFacetNode("image2.jpg", image2.getIdentifier());

        model.setObject("Text with only one image: <img src=\"/binaries/image1.jpg/{_document}/hippogallery:thumbnail\" data-uuid=\"" + image1.getIdentifier() + "\" data-type=\"hippogallery:thumbnail\" />");

        NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have only one facet child node", 1, documentNode.getNodes().getSize());

        Node child = children.nextNode();
        assertEquals(image1.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("Text with only one image: <img src=\"image1.jpg/{_document}/hippogallery:thumbnail\" />", textModel.getObject());
    }

    @Test
    public void setTextWithLinksRemovesUnusedChildNodesWithAdditionalSuffix() throws RepositoryException {
        Node document = rootNode.addNode("document1", "nt:unstructured");

        addChildFacetNode("document", document.getIdentifier());
        addChildFacetNode("document_1", document.getIdentifier());

        model.setObject("");
        model.setObject("<a href=\"http://\" data-uuid=\"" + document.getIdentifier() + "\">document</a>");

        NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have only one facet child node", 1, documentNode.getNodes().getSize());

        Node child = children.nextNode();
        assertEquals(document.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<a href=\"" + child.getName() + "\">document</a>", textModel.getObject());
    }

    @Test
    public void setNewLinkUuidForTargetWithSameNameCreatesChildNodeWithDifferentName() throws RepositoryException {
        Node document1 = rootNode.addNode("folder-one", "nt:unstructured").addNode("document", "nt:unstructured");
        Node document2 = rootNode.addNode("folder-two", "nt:unstructured").addNode("document", "nt:unstructured");

        addChildFacetNode("document", document1.getIdentifier());

        model.setObject("");
        model.setObject("<a href=\"http://\" data-uuid=\"" + document1.getIdentifier() + "\">document one</a>" +
                " and <a href=\"http://\" data-uuid=\"" + document2.getIdentifier() + "\">document two</a>");

        NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have two facet child nodes", 2, documentNode.getNodes().getSize());

        Node child1 = children.nextNode();
        assertEquals(document1.getIdentifier(), child1.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        Node child2 = children.nextNode();
        assertEquals(document2.getIdentifier(), child2.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertFalse("Children should have different names", child1.getName().equals(child2.getName()));

        assertEquals("<a href=\"" + child1.getName() + "\">document one</a>" +
                        " and <a href=\"" + child2.getName() + "\">document two</a>",
                textModel.getObject());
    }

    @Test
    public void setEmptyTextRemovesPreviouslyCreatedChildNodes() throws RepositoryException {
        Node linked = rootNode.addNode("linked", "nt:unstructured");

        model.setObject("");

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

        ChildFacetUuidsModel model = new ChildFacetUuidsModel(delegate, nodeModel, linkFactory, null);

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

    @Test
    public void getTextChangesSrcAndAddsFacetSelectAndType() throws RepositoryException {
        addChildFacetNode("image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");
        textModel.setObject("<img src=\"image.jpg/{_document}/hippogallery:original\" />");
        assertEquals("<img src=\"/binaries/image.jpg/{_document}/hippogallery:original\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\" data-type=\"hippogallery:original\" />", model.getObject());
    }

    @Test
    public void setTextRestoresSrcAndRemovesFacetSelectAndType() throws RepositoryException {
        addChildFacetNode("image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");
        model.setObject("<img src=\"/binaries/image.jpg/{_document}/hippogallery:original\" data-facetselect=\"image.jpg/{_document}/hippogallery:original\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\" data-type=\"hippogallery:original\" />");
        assertEquals("<img src=\"image.jpg/{_document}/hippogallery:original\" />", textModel.getObject());
    }

    @Test
    public void getSrcWithoutVariantOmitsType() throws RepositoryException {
        addChildFacetNode("image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");
        textModel.setObject("<img src=\"image.jpg\" />");
        assertEquals("<img src=\"/binaries/image.jpg\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\" />", model.getObject());
    }

    @Test
    public void setSrcWithoutVariantRemovesFacetSelect() throws RepositoryException {
        addChildFacetNode("image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");
        model.setObject("<img src=\"/binaries/image.jpg\" data-facetselect=\"image.jpg\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\" />");
        assertEquals("<img src=\"image.jpg\" />", textModel.getObject());
    }

    @Test
    public void additionalImgAttributesAreNotChanged() throws RepositoryException {
        addChildFacetNode("image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");
        model.setObject("<img src=\"/binaries/image.jpg/{_document}/hippogallery:original\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\" data-type=\"hippogallery:original\" align=\"right\" />");
        assertEquals("<img src=\"/binaries/image.jpg/{_document}/hippogallery:original\" align=\"right\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\" data-type=\"hippogallery:original\" />", model.getObject());
    }

    @Test
    public void externalImageWithEndTagChangesToClosedOne() {
        model.setObject("<img src=\"http://www.example.com/image.jpg\"></img>");
        assertEquals("<img src=\"http://www.example.com/image.jpg\" />", model.getObject());
    }

    @Test
    public void getRichTextImageHasCorrectUrl() throws RepositoryException, RichTextException {
        addChildFacetNode("image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");

        final RichTextImage image = new RichTextImage("/content/gallery/image.jpg/image.jpg", "image.jpg");
        image.setSelectedResourceDefinition("hippogallery:original");

        final IRichTextImageFactory mockImageFactory = EasyMock.createMock(IRichTextImageFactory.class);
        expect(mockImageFactory.loadImageItem(eq("image.jpg/{_document}/hippogallery:original"))).andReturn(image);

        final IRichTextLinkFactory mockLinkFactory = EasyMock.createMock(IRichTextLinkFactory.class);
        expect(mockLinkFactory.getLinkUuids()).andReturn(Collections.singleton("0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21"));

        final RichTextImageURLProvider urlProvider = new RichTextImageURLProvider(mockImageFactory, mockLinkFactory, documentNodeModel);

        replay(mockImageFactory, mockLinkFactory);

        textModel.setObject("<img src=\"image.jpg/{_document}/hippogallery:original\" />");
        model = new ChildFacetUuidsModel(textModel, documentNodeModel, mockLinkFactory, urlProvider);

        assertEquals("<img src=\"binaries/content/gallery/image.jpg/image.jpg/hippogallery:original\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\" data-type=\"hippogallery:original\" />", model.getObject());
    }

    @Test
    public void setDocumentsWithTheSameName() throws RepositoryException {
        Node doc1 = rootNode.addNode("doc", "nt:unstructured");
        Node doc2 = rootNode.addNode("doc", "nt:unstructured");
        model.setObject("<a href=\"http://\" data-uuid=\"" + doc1.getIdentifier() + "\"></a>" +
                "<a href=\"http://\" data-uuid=\"" + doc2.getIdentifier() + "\"></a>");
        assertTrue("facetselect node doc exists", documentNode.hasNode("doc"));
        assertTrue("facetselect node doc_1 exists", documentNode.hasNode("doc_1"));
        assertEquals("<a href=\"doc\"></a><a href=\"doc_1\"></a>", textModel.getObject());
    }

    @Test
    public void setImagesWithTheSameName() throws RepositoryException {
        Node image1 = rootNode.addNode("image.jpg", "nt:unstructured");
        Node image2 = rootNode.addNode("image.jpg", "nt:unstructured");
        model.setObject("<img src=\"/binaries/image.jpg\" data-uuid=\"" + image1.getIdentifier() + "\" data-type=\"hippogallery:original\" />" +
                "<img src=\"/binaries/image.jpg\" data-uuid=\"" + image2.getIdentifier() + "\" data-type=\"hippogallery:original\" />");
        assertTrue("facetselect node image.jpg exists", documentNode.hasNode("image.jpg"));
        assertTrue("facetselect node image.jpg_1 exists", documentNode.hasNode("image.jpg_1"));
        assertEquals("<img src=\"image.jpg/{_document}/hippogallery:original\" /><img src=\"image.jpg_1/{_document}/hippogallery:original\" />", textModel.getObject());
    }

    private class PrefixingImageUrlProvider implements IImageURLProvider {

        private String prefix;

        PrefixingImageUrlProvider(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String getURL(String link) throws RichTextException {
            return prefix + "/" + link;
        }
    }

}
