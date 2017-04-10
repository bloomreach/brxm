/*
* Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.easymock.EasyMock;
import org.hamcrest.CoreMatchers;
import org.hippoecm.frontend.plugins.richtext.processor.WicketModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.junit.Before;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.onehippo.cms7.services.processor.html.HtmlProcessor;
import org.onehippo.cms7.services.processor.html.HtmlProcessorConfig;
import org.onehippo.cms7.services.processor.html.HtmlProcessorImpl;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.html.serialize.HtmlSerializer;
import org.onehippo.cms7.services.processor.richtext.RichTextException;
import org.onehippo.cms7.services.processor.richtext.URLProvider;
import org.onehippo.cms7.services.processor.richtext.image.RichTextImageFactory;
import org.onehippo.cms7.services.processor.richtext.jcr.JcrNodeFactory;
import org.onehippo.cms7.services.processor.richtext.link.RichTextLinkFactory;
import org.onehippo.cms7.services.processor.richtext.model.RichTextProcessorModel;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RichTextModelTest {

    private MockNode rootNode;
    private MockNode documentNode;

    private IModel<String> textModel;
    private RichTextModel richTextModel;

    @Before
    public void setUp() throws RepositoryException, RichTextException {
        rootNode = MockNode.root();

        documentNode = new MockNode("document");
        rootNode.addNode(documentNode);

        final IModel<Node> documentNodeModel = EasyMock.createMock(IModel.class);
        expect(documentNodeModel.getObject()).andStubReturn(documentNode);
        replay(documentNodeModel);

        textModel = org.apache.wicket.model.Model.of("");

        final Model<String> valueModel = WicketModel.of(textModel);
        final Model<Node> nodeModel = WicketModel.of(documentNodeModel);

        final JcrNodeFactory nodeFactory = new JcrNodeFactory(() -> rootNode.getSession()) {
            @Override
            public Model<Node> getNodeModelByIdentifier(final String uuid) throws RepositoryException {
                return null;
            }
        };

        final RichTextProcessorModel processorModel = new RichTextProcessorModel(valueModel, nodeModel,
                                                                                 RichTextModelTest::createHtmlProcessor,
                                                                                 nodeFactory) {
            @Override
            protected URLProvider createImageURLProvider(final Model<Node> nodeModel,
                                                         final RichTextLinkFactory linkFactory,
                                                         final RichTextImageFactory richTextImageFactory) {
                return new PrefixingImageUrlProvider("/binaries");
            }
        };

        richTextModel = new RichTextModel(processorModel);
    }

    private static HtmlProcessor createHtmlProcessor() {
        final HtmlProcessorConfig htmlProcessorConfig = new HtmlProcessorConfig();
        htmlProcessorConfig.setCharset("UTF-8");
        htmlProcessorConfig.setSerializer(HtmlSerializer.SIMPLE);
        htmlProcessorConfig.setOmitComments(false);
        htmlProcessorConfig.setConvertLineEndings(false);
        return new HtmlProcessorImpl(htmlProcessorConfig);
    }

    private void addChildFacetNode(final String name, final String uuid) throws RepositoryException {
        final Node child = documentNode.addNode(name, HippoNodeType.NT_FACETSELECT);
        child.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
    }

    private void assertSetTextUnchangedAndAllChildFacetsRemoved(final String text) throws RepositoryException {
        richTextModel.setObject(text);
        assertEquals("all child facet nodes should have been removed", 0, documentNode.getNodes().getSize());
        assertEquals(emptyIfNull(text), textModel.getObject());
    }

    private void assertNoChanges(final String text) throws RepositoryException {
        textModel.setObject(text);
        assertEquals("Stored text should be returned without changes", emptyIfNull(text), richTextModel.getObject());

        richTextModel.setObject(text);
        assertEquals("Text should be stored without changes", emptyIfNull(text), textModel.getObject());
        assertEquals("Number of child facet nodes should not have changed", documentNode.getNodes().getSize(), documentNode.getNodes().getSize());
    }

    private String emptyIfNull(final String text) {
        if (text == null) {
            return "";
        }
        return text;
    }

    @Test
    public void nullTextDoesNotChange() throws RepositoryException {
        assertNoChanges(null);
    }

    @Test
    public void emptyTextDoesNotChange() throws RepositoryException {
        assertNoChanges("");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void getLinkChildNodeNameIsRewrittenToUuid() throws RepositoryException {
        addChildFacetNode("linked-node", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        textModel.setObject("<a href=\"linked-node\">link</a>");
        assertEquals("<a href=\"http://\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\">link</a>", richTextModel.getObject());
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void getLinkWithEscapedNameIsRewrittenToUuid() throws RepositoryException {
        final String name = "A name that needs 'encoding'";
        final String linkTargetName = NodeNameCodec.encode(name, true);
        assertFalse(name.equals(linkTargetName));

        addChildFacetNode(linkTargetName, "d1b804c0-cf19-451f-8c0f-184da74289e4");

        textModel.setObject("<a href=\"" + linkTargetName + "\">link</a>");

        assertEquals("<a href=\"http://\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\">link</a>", richTextModel.getObject());
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void getImageChildNodeNameIsRewrittenToUuid() throws RepositoryException {
        addChildFacetNode("image.jpg", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        textModel.setObject("<img src=\"image.jpg/{_document}/hippogallery:thumbnail\" />");
        assertEquals("<img src=\"/binaries/image.jpg/{_document}/hippogallery:thumbnail\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\" data-type=\"hippogallery:thumbnail\" />", richTextModel.getObject());
    }

    // TODO: can be removed, not moved to LinkVisitorTest
    @Test
    public void getMultipleLinkChildNodeNamesAreRewrittenToUuids() throws RepositoryException {
        addChildFacetNode("linked-node-1", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        addChildFacetNode("linked-node-2", "eb40e696-67db-4d5b-a09a-987e6c49543d");

        textModel.setObject("Two links: <a href=\"linked-node-1\">one</a> and <a href=\"linked-node-2\">two</a>");

        assertEquals("Two links: <a href=\"http://\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\">one</a> and <a href=\"http://\" data-uuid=\"eb40e696-67db-4d5b-a09a-987e6c49543d\">two</a>", richTextModel.getObject());
    }

    // TODO: can be removed, not moved to imageVisitorTest
    @Test
    public void getMultipleImageChildNodeNamesAreRewrittenToUuids() throws RepositoryException {
        addChildFacetNode("foo.jpg", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        addChildFacetNode("bar.jpg", "eb40e696-67db-4d5b-a09a-987e6c49543d");
        textModel.setObject("Two images: <img src=\"foo.jpg/{_document}/hippogallery:original\" /> and <img src=\"bar.jpg/{_document}/hippogallery:original\" />");
        assertEquals("Two images: <img src=\"/binaries/foo.jpg/{_document}/hippogallery:original\" data-uuid=\"d1b804c0-cf19-451f-8c0f-184da74289e4\" data-type=\"hippogallery:original\" /> and <img src=\"/binaries/bar.jpg/{_document}/hippogallery:original\" data-uuid=\"eb40e696-67db-4d5b-a09a-987e6c49543d\" data-type=\"hippogallery:original\" />", richTextModel.getObject());
    }

    // TODO: can be removed, not moved to imageVisitorTest
    @Test
    public void getMissingImageChildNodeDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"no-such-image.jpg/{_document}/hippogallery:original\" />");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void anchorDoesNotChange() throws RepositoryException {
        assertNoChanges("<a name=\"foo\">anchor</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void relativeLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"somepage.html\">relative link</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void relativeLinkWithPathDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"../somepage.html\">relative link with path</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void relativeLinkWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"2*3=6.html\">Link to file with illegal JCR characters in its name</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void externalHttpLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"http://www.example.com\">external link</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void externalLinkWithPortDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"http://www.example.com:8080\">external link with port</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void externalHttpOnlyLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"http://\">strange external link</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void linkWithEmptyHrefDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"\">link with empty href</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void linkWithEmptyHrefAndUuidDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"\" data-uuid=\"\">link with empty href and uuid</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void externalFtpLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"ftp://www.example.com\">external FTP link</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void externalFtpOnlyLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a href=\"ftp://\">strange external FTP link</a>");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void emptyLinkDoesNotChange() throws RepositoryException {
        assertNoChanges("<a>strange empty link</a>");
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void externalImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"http://www.example.com/foo.jpg\" />");
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void externalHttpOnlyImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"http://\" />");
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void externalRelativeImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"images/foo.jpg\" />");
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void externalRelativeWithPathImageDoesNotChange() throws RepositoryException {
        assertNoChanges("<img src=\"../images/foo.jpg\" />");
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void externalImageWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("<a><img src=\"2*3=6.png\" />Link to image illegal JCR characters in its name</a>");
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void externalRelativeImageWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("100% correct image: \n<img src=\"100%25+correct.png\" />");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void setNewLinkUuidCreatesChildNodeAndReplacesUuid() throws RepositoryException {
        final Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");

        richTextModel.setObject("");
        assertEquals(0, documentNode.getNodes().getSize());

        richTextModel.setObject("<a href=\"http://\" data-uuid=\"" + linkTarget.getIdentifier() + "\">link</a>");
        assertTrue("Text with new link UUID should create a child facet node", documentNode.hasNode("linked-node"));
        assertEquals("Text with new link UUID should create exactly one child facet node", 1, documentNode.getNodes().getSize());

        final Node child = documentNode.getNode("linked-node");
        assertEquals(linkTarget.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<a href=\"linked-node\">link</a>", textModel.getObject());
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void setNewImageUuidCreatesChildNodeAndRemovesUuid() throws RepositoryException {
        final Node linkedImage = rootNode.addNode("linked-image.jpg", "nt:unstructured");

        richTextModel.setObject("");
        assertEquals(0, documentNode.getNodes().getSize());

        richTextModel.setObject("<img src=\"/binaries/linked-image.jpg/linked-image.jpg/hippogallery:thumbnail\" data-uuid=\"" + linkedImage.getIdentifier() + "\" data-type=\"hippogallery:thumbnail\" />");
        assertTrue("Text with new image UUID should create a child facet node", documentNode.hasNode("linked-image.jpg"));
        assertEquals("Text with new image UUID should create exactly one child facet node", 1, documentNode.getNodes().getSize());

        final Node child = documentNode.getNode("linked-image.jpg");
        assertEquals(linkedImage.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<img src=\"linked-image.jpg/{_document}/hippogallery:thumbnail\" />", textModel.getObject());
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void setExistingLinkUuidDoesNotCreateChildNode() throws RepositoryException {
        final Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        richTextModel.setObject("");
        richTextModel.setObject("<a href=\"http://\" data-uuid=\"" + linkTarget.getIdentifier() + "\">link</a>");

        assertEquals("Text with existing link UUID should reuse existing child facet node", 1, documentNode.getNodes().getSize());

        final Node child = documentNode.getNode("linked-node");
        assertEquals(linkTarget.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<a href=\"linked-node\">link</a>", textModel.getObject());
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void setExistingImageUuidDoesNotCreateChildNode() throws RepositoryException {
        final Node linkedImage = rootNode.addNode("linked-image.jpg", "nt:unstructured");
        addChildFacetNode("linked-image.jpg", linkedImage.getIdentifier());

        richTextModel.setObject("<img src=\"/binaries/linked-image.jpg/linked-image.jpg/hippogallery:thumbnail\" data-uuid=\"" + linkedImage.getIdentifier() + "\" data-type=\"hippogallery:thumbnail\" />");

        assertEquals("Text with existing image UUID should reuse existing child facet node", 1, documentNode.getNodes().getSize());

        final Node child = documentNode.getNode("linked-image.jpg");
        assertEquals(linkedImage.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("<img src=\"linked-image.jpg/{_document}/hippogallery:thumbnail\" />", textModel.getObject());
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void setExternalLinkWithUuidIgnoresUuid() throws RepositoryException {
        final Node document1 = rootNode.addNode("document1", "nt:unstructured");
        richTextModel.setObject("");
        richTextModel.setObject("<a href=\"http://www.example.com\" data-uuid=\"" + document1.getIdentifier() + "\">external link</a>");
        assertEquals("No child facet nodes should have been created", 0, documentNode.getNodes().getSize());
        assertEquals("<a href=\"http://www.example.com\">external link</a>", textModel.getObject());
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void setTextWithoutAnyLinksRemovesAllChildNodes() throws RepositoryException {
        final Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        assertSetTextUnchangedAndAllChildFacetsRemoved("Text without link");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void setEmptyTextRemovesAllChildNodes() throws RepositoryException {
        final Node linkTarget = rootNode.addNode("linked-node", "nt:unstructured");
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        assertSetTextUnchangedAndAllChildFacetsRemoved("");
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void setNullTextRemovesAllChildNodes() throws RepositoryException {
        final Node linkTarget = rootNode.addNode("linked-node", HippoNodeType.NT_FACETSELECT);
        addChildFacetNode("linked-node", linkTarget.getIdentifier());

        assertSetTextUnchangedAndAllChildFacetsRemoved(null);
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void setTextWithLinksRemovesUnusedChildNodes() throws RepositoryException {
        final Node document1 = rootNode.addNode("document1", "nt:unstructured");
        final Node document2 = rootNode.addNode("document2", "nt:unstructured");

        addChildFacetNode("document1", document1.getIdentifier());
        addChildFacetNode("document2", document2.getIdentifier());

        richTextModel.setObject("");
        richTextModel.setObject("Text with only one link to <a href=\"http://\" data-uuid=\"" + document1.getIdentifier() + "\">document one</a>");

        final NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have only one facet child node", 1, documentNode.getNodes().getSize());

        final Node child = children.nextNode();
        assertEquals(document1.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("Text with only one link to <a href=\"" + child.getName() + "\">document one</a>", textModel.getObject());
    }

    // TODO: moved to ImageVisitorTest
    @Test
    public void setTextWithImagesRemovesUnusedChildNodes() throws RepositoryException {
        final Node image1 = rootNode.addNode("image1.jpg", "nt:unstructured");
        final Node image2 = rootNode.addNode("image2.jpg", "nt:unstructured");

        addChildFacetNode("image1.jpg", image1.getIdentifier());
        addChildFacetNode("image2.jpg", image2.getIdentifier());

        richTextModel.setObject("Text with only one image: <img src=\"/binaries/image1.jpg/{_document}/hippogallery:thumbnail\" data-uuid=\"" + image1.getIdentifier() + "\" data-type=\"hippogallery:thumbnail\" />");

        final NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have only one facet child node", 1, documentNode.getNodes().getSize());

        final Node child = children.nextNode();
        assertEquals(image1.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertEquals("Text with only one image: <img src=\"image1.jpg/{_document}/hippogallery:thumbnail\" />", textModel.getObject());
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void setTextWithLinksRemovesUnusedChildNodesWithAdditionalSuffix() throws RepositoryException {
        final Node document = rootNode.addNode("document1", "nt:unstructured");

        addChildFacetNode("document", document.getIdentifier());
        addChildFacetNode("document_1", document.getIdentifier());

        richTextModel.setObject("");
        richTextModel.setObject("<a href=\"http://\" data-uuid=\"" + document.getIdentifier() + "\">document</a>");

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

        richTextModel.setObject("");
        richTextModel.setObject("<a href=\"http://\" data-uuid=\"" + document1.getIdentifier() + "\">document one</a>" +
                " and <a href=\"http://\" data-uuid=\"" + document2.getIdentifier() + "\">document two</a>");

        final NodeIterator children = documentNode.getNodes();
        assertEquals("Document node should have two facet child nodes", 2, documentNode.getNodes().getSize());


        final Node child1 = children.nextNode();
        final Node child2 = children.nextNode();
        assertThat(
                Arrays.asList(
                        child1.getProperty(HippoNodeType.HIPPO_DOCBASE).getString(),
                        child2.getProperty(HippoNodeType.HIPPO_DOCBASE).getString()
                ),
                JUnitMatchers.hasItems(
                        CoreMatchers.equalTo(document1.getIdentifier()),
                        CoreMatchers.equalTo(document2.getIdentifier())
                )
        );

        assertFalse("Children should have different names", child1.getName().equals(child2.getName()));

        assertThat(textModel.getObject(), CoreMatchers.allOf(
                JUnitMatchers.containsString("<a href=\"" + child1.getName() + "\">"),
                JUnitMatchers.containsString("<a href=\"" + child2.getName() + "\">")
        ));
    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void setEmptyTextRemovesPreviouslyCreatedChildNodes() throws RepositoryException {
        final Node linked = rootNode.addNode("linked", "nt:unstructured");

        richTextModel.setObject("");

        richTextModel.setObject("<a href=\"http://\" data-uuid=\"" + linked.getIdentifier() + "\">linked</a>");
        assertEquals("Child facet node should have been added", 1, documentNode.getNodes().getSize());

        richTextModel.setObject("");
        assertEquals("Child facet node should have been removed", 0, documentNode.getNodes().getSize());
    }

//    @Test
//    public void allStateIsDetached() {
//        IModel<String> delegate = EasyMock.createMock(IModel.class);
//        IModel<Node> nodeModel = EasyMock.createMock(IModel.class);
//
//        NodeProvider nodeModelProvider = NodeProvider.of(nodeModel);
//        List<TagVisitor> visitors = Lists.newArrayList(new LinkVisitor(nodeModelProvider));
//
//        HtmlProcessorProvider provider = new HtmlProcessorProvider() {
//            @Override
//            public HtmlProcessor getProcessor() {
//                return createHtmlProcessor();
//            }
//
//            @Override
//            public List<TagVisitor> getVisitors() {
//                return visitors;
//            }
//
//            @Override
//            public void detach() {
//                visitors.forEach(TagVisitor::detach);
//            }
//        };
//        RichTextModel model = new RichTextModel(delegate, provider);
//
//        delegate.detach();
//        expectLastCall();
//
//        nodeModel.detach();
//        expectLastCall();
//
//        replay(delegate, nodeModel);
//
//        model.detach();
//
//        verify(delegate, nodeModel);
//    }


    // TODO: moved to imageVisitorTest
    @Test
    public void getTextChangesSrcAndAddsFacetSelectAndType() throws RepositoryException {
        addChildFacetNode("image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");
        textModel.setObject("<img src=\"image.jpg/{_document}/hippogallery:original\" />");
        assertEquals("<img src=\"/binaries/image.jpg/{_document}/hippogallery:original\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\" data-type=\"hippogallery:original\" />", richTextModel.getObject());
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void getSrcWithoutVariantOmitsType() throws RepositoryException {
        addChildFacetNode("image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");
        textModel.setObject("<img src=\"image.jpg\" />");
        assertEquals("<img src=\"/binaries/image.jpg\" data-uuid=\"0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21\" />", richTextModel.getObject());
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void additionalImgAttributesAreNotChanged() throws RepositoryException {
        final Node image = rootNode.addNode("image.jpg", "nt:unstructured");
        addChildFacetNode("image.jpg", image.getIdentifier());
        richTextModel.setObject("<img src=\"/binaries/image.jpg/{_document}/hippogallery:original\" data-uuid=\"" + image.getIdentifier() + " \" data-type=\"hippogallery:original\" align=\"right\" />");
        assertEquals("<img src=\"/binaries/image.jpg/{_document}/hippogallery:original\" align=\"right\" data-uuid=\"" + image.getIdentifier() + "\" data-type=\"hippogallery:original\" />", richTextModel.getObject());
    }

    @Test
    public void externalImageWithEndTagChangesToClosedOne() {
        richTextModel.setObject("<img src=\"http://www.example.com/image.jpg\"></img>");
        assertEquals("<img src=\"http://www.example.com/image.jpg\" />", richTextModel.getObject());
    }

// TODO: moved to imageVisitorTest
//    @Test
//    public void getRichTextImageHasCorrectUrl() throws RepositoryException, RichTextException {
//        Node path = rootNode.addNode("path", "nt:folder");
//        Node image = path.addNode("image.jpg", "nt:unstructured");
//        addChildFacetNode("image.jpg", image.getIdentifier());
//
//        final RichTextImage richTextImage = new RichTextImage("/path/image.jpg/image.jpg", "image.jpg");
//        richTextImage.setSelectedResourceDefinition("hippogallery:original");
//
//        final IRichTextImageFactory mockImageFactory = EasyMock.createMock(IRichTextImageFactory.class);
//        expect(mockImageFactory.loadImageItem(eq(image.getIdentifier()), eq("hippogallery:original"))).andReturn(richTextImage);
//
//        final IRichTextLinkFactory mockLinkFactory = EasyMock.createMock(IRichTextLinkFactory.class);
//        expect(mockLinkFactory.getLinkUuids()).andReturn(Collections.singleton(image.getIdentifier()));
//
//        replay(mockImageFactory, mockLinkFactory);
//        final RichTextImageURLProvider urlProvider = new RichTextImageURLProvider(mockImageFactory, mockLinkFactory, documentNodeModel);
//
//        NodeProvider nodeModelProvider = NodeProvider.of(documentNodeModel);
//        final ImageVisitor imageVisitor = new ImageVisitor(nodeModelProvider, new RichTextImageLinkProvider(urlProvider));
//        // converterBuilder = new UuidConverterBuilder(documentNodeModel, mockLinkFactory, urlProvider);
//
//        textModel.setObject("<img src=\"image.jpg/{_document}/hippogallery:original\" />");
//
//        HtmlProcessorProvider provider = new HtmlProcessorProvider() {
//            @Override
//            public HtmlProcessor getProcessor() {
//                return createHtmlProcessor();
//            }
//
//            @Override
//            public List<TagVisitor> getVisitors() {
//                return Lists.newArrayList(imageVisitor);
//            }
//
//            @Override
//            public void detach() {
//
//            }
//        };
//
//        richTextModel = new RichTextModel(textModel, provider);
//
//        assertEquals("<img src=\"binaries/path/image.jpg/image.jpg/hippogallery:original\" data-uuid=\"" + image.getIdentifier() + "\" data-type=\"hippogallery:original\" />", richTextModel.getObject());
//    }

    // TODO: moved to LinkVisitorTest
    @Test
    public void setDocumentsWithTheSameName() throws RepositoryException {
        final Node doc1 = rootNode.addNode("doc", "nt:unstructured");
        final Node doc2 = rootNode.addNode("doc", "nt:unstructured");
        richTextModel.setObject("<a href=\"http://\" data-uuid=\"" + doc1.getIdentifier() + "\"></a>" +
                "<a href=\"http://\" data-uuid=\"" + doc2.getIdentifier() + "\"></a>");
        assertTrue("facetselect node doc exists", documentNode.hasNode("doc"));
        assertTrue("facetselect node doc_1 exists", documentNode.hasNode("doc_1"));
        assertEquals("<a href=\"doc\"></a><a href=\"doc_1\"></a>", textModel.getObject());
    }

    // TODO: moved to imageVisitorTest
    @Test
    public void setImagesWithTheSameName() throws RepositoryException {
        final Node image1 = rootNode.addNode("image.jpg", "nt:unstructured");
        final Node image2 = rootNode.addNode("image.jpg", "nt:unstructured");
        richTextModel.setObject("<img src=\"/binaries/image.jpg\" data-uuid=\"" + image1.getIdentifier() + "\" data-type=\"hippogallery:original\" />" +
                "<img src=\"/binaries/image.jpg\" data-uuid=\"" + image2.getIdentifier() + "\" data-type=\"hippogallery:original\" />");
        assertTrue("facetselect node image.jpg exists", documentNode.hasNode("image.jpg"));
        assertTrue("facetselect node image.jpg_1 exists", documentNode.hasNode("image.jpg_1"));
        assertEquals("<img src=\"image.jpg/{_document}/hippogallery:original\" /><img src=\"image.jpg_1/{_document}/hippogallery:original\" />", textModel.getObject());
    }

    @Test
    public void htmlEntitiesArePreserved() {
        testPreserved("&gt;&lt;&amp;&nbsp;");
    }

    @Test
    public void htmlCommentsArePreserved() {
        testPreserved("<!-- comment -->");
    }

    @Test
    public void utfCharactersArePreserved() {
        testPreserved("łФ௵سლ");
    }

    @Test
    public void codeBlockIsPreserved() {
        testPreserved("<pre class=\"sh_xml\">&lt;hst:defineObjects/&gt;\n" +
                "&lt;c:set var=\"isPreview\" value=\"${hstRequest.requestContext.preview}\"/&gt;\n" +
                "</pre>");
    }

    private void testPreserved(final String html) {
        richTextModel.setObject(html);
        assertEquals(html, richTextModel.getObject());
    }

    private class PrefixingImageUrlProvider implements URLProvider {

        private final String prefix;

        PrefixingImageUrlProvider(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String getURL(final String link) {
            return prefix + "/" + link;
        }
    }

}
