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
package org.onehippo.cms7.services.processor.richtext.visit;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.easymock.EasyMock;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.html.visit.Tag;
import org.onehippo.cms7.services.processor.richtext.UrlProvider;
import org.onehippo.cms7.services.processor.richtext.jcr.JcrNodeFactory;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ImageVisitorTest {

    private MockNode root;
    private MockNode document;
    private Model<Node> documentModel;
    private PrefixingImageUrlProvider prefixingImageUrlProvider;

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
        prefixingImageUrlProvider = new PrefixingImageUrlProvider("/binaries");
    }

    @Test
    public void testVisitsOnlyImgElements() throws Exception {
        final ImageVisitor visitor = new ImageVisitor(documentModel, src -> src);

        final Tag image = EasyMock.createMock(Tag.class);
        expect(image.getName()).andReturn("div").times(2);
        EasyMock.replay(image);

        visitor.visitBeforeRead(null, image);
        visitor.visitBeforeWrite(null, image);

        EasyMock.verify(image);
    }

    @Test
    public void externalImageDoesNotChange() throws RepositoryException {
        assertNoChanges("http://www.example.com/foo.jpg");
    }

    @Test
    public void externalHttpOnlyImageDoesNotChange() throws RepositoryException {
        assertNoChanges("http://");
    }

    @Test
    public void externalRelativeImageDoesNotChange() throws RepositoryException {
        assertNoChanges("images/foo.jpg");
    }

    @Test
    public void externalRelativeWithPathImageDoesNotChange() throws RepositoryException {
        assertNoChanges("../images/foo.jpg");
    }

    @Test
    public void externalImageWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("2*3=6.png");
    }

    @Test
    public void externalRelativeImageWithIllegalJcrCharsDoesNotChange() throws RepositoryException {
        assertNoChanges("100%25+correct.png");
    }

    @Test
    public void getMissingImageChildNodeDoesNotChange() throws RepositoryException {
        assertNoChanges("no-such-image.jpg/{_document}/hippogallery:original");
    }

    @Test
    public void testReadRichTextImage() throws Exception {
        addChildFacetNode("foo.jpg", "d1b804c0-cf19-451f-8c0f-184da74289e4");

        final Tag image = createImage("foo.jpg/{_document}/hippogallery:original");
        read(image, src -> src);

        assertImage(image,
                    "foo.jpg/{_document}/hippogallery:original",
                    "d1b804c0-cf19-451f-8c0f-184da74289e4",
                    "hippogallery:original");
    }

    @Test
    public void getImageChildNodeNameIsRewrittenToUuid() throws RepositoryException {
        addChildFacetNode("image.jpg", "d1b804c0-cf19-451f-8c0f-184da74289e4");
        final Tag image = createImage("image.jpg/{_document}/hippogallery:thumbnail");

        read(image);

        assertImage(image,
                    "/binaries/image.jpg/{_document}/hippogallery:thumbnail",
                    "d1b804c0-cf19-451f-8c0f-184da74289e4",
                    "hippogallery:thumbnail");
    }

    @Test
    public void setNewImageUuidCreatesChildNodeAndRemovesUuid() throws RepositoryException {
        final Node linkedImage = root.addNode("linked-image.jpg", "nt:unstructured");

        final Tag image = createImage("/binaries/linked-image.jpg/linked-image.jpg/hippogallery:thumbnail");
        image.addAttribute("data-uuid", linkedImage.getIdentifier());
        image.addAttribute("data-type", "hippogallery:thumbnail");

        write(image, src -> src);

        assertTrue("Text with new image UUID should create a child facet node", document.hasNode("linked-image.jpg"));
        assertEquals("Text with new image UUID should create exactly one child facet node", 1, document.getNodes().getSize());

        final Node child = document.getNode("linked-image.jpg");
        assertEquals(linkedImage.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertImage(image, "linked-image.jpg/{_document}/hippogallery:thumbnail");
    }

    @Test
    public void setExistingImageUuidDoesNotCreateChildNode() throws RepositoryException {
        final Node linkedImage = root.addNode("linked-image.jpg", "nt:unstructured");
        addChildFacetNode("linked-image.jpg", linkedImage.getIdentifier());

        final Tag image = createImage("/binaries/linked-image.jpg/linked-image.jpg/hippogallery:thumbnail");
        image.addAttribute("data-uuid", linkedImage.getIdentifier());
        image.addAttribute("data-type", "hippogallery:thumbnail");
        write(image, src -> src);

        assertEquals("Text with existing image UUID should reuse existing child facet node", 1, document.getNodes().getSize());

        final Node child = document.getNode("linked-image.jpg");
        assertEquals(linkedImage.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());

        assertImage(image, "linked-image.jpg/{_document}/hippogallery:thumbnail");
    }

    @Test
    public void getTextChangesSrcAndAddsFacetSelectAndType() throws RepositoryException {
        addChildFacetNode("image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");

        final Tag image = createImage("image.jpg/{_document}/hippogallery:original");
        read(image);

        assertImage(image, "/binaries/image.jpg/{_document}/hippogallery:original",
                           "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21",
                           "hippogallery:original");
    }

    @Test
    public void getSrcWithoutVariantOmitsType() throws RepositoryException {
        addChildFacetNode("image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");

        final Tag image = createImage("image.jpg");
        read(image);

        assertImage(image, "/binaries/image.jpg", "0e8a928c-b83f-4bb9-9e52-1a22b7e9ee21");
    }

    @Test
    public void additionalImgAttributesAreNotChanged() throws RepositoryException {
        final Node imageNode = root.addNode("image.jpg", "nt:unstructured");
        addChildFacetNode("image.jpg", imageNode.getIdentifier());

        final Tag image = createImage("/binaries/image.jpg/{_document}/hippogallery:original");
        image.addAttribute("data-uuid", imageNode.getIdentifier());
        image.addAttribute("data-type", "hippogallery:original");
        image.addAttribute("align", "right");

        write(image);
        read(image);

        assertImage(image, "/binaries/image.jpg/{_document}/hippogallery:original",
                    imageNode.getIdentifier(), "hippogallery:original");
        assertEquals("right", image.getAttribute("align"));
    }

    @Test
    public void setImagesWithTheSameName() throws RepositoryException {
        final Node imageNode1 = root.addNode("image.jpg", "nt:unstructured");
        final Node imageNode2 = root.addNode("image.jpg", "nt:unstructured");
        final Tag div = new TestTag("div");

        final Tag image1 = createImage("/binaries/image.jpg");
        image1.addAttribute("data-uuid", imageNode1.getIdentifier());
        image1.addAttribute("data-type", "hippogallery:original");

        final Tag image2 = createImage("/binaries/image.jpg");
        image2.addAttribute("data-uuid", imageNode2.getIdentifier());
        image2.addAttribute("data-type", "hippogallery:original");

        final ImageVisitor visitor = new ImageVisitor(documentModel, prefixingImageUrlProvider);
        visitor.visitBeforeWrite(div, image1);
        visitor.visitBeforeWrite(div, image2);

        assertTrue("facetselect node image.jpg exists", document.hasNode("image.jpg"));
        assertTrue("facetselect node image.jpg_1 exists", document.hasNode("image.jpg_1"));

        assertImage(image1, "image.jpg/{_document}/hippogallery:original");
        assertImage(image2, "image.jpg_1/{_document}/hippogallery:original");
    }

    @Test
    public void setTextWithImagesRemovesUnusedChildNodes() throws RepositoryException {
        final Node imageNode1 = root.addNode("image1.jpg", "nt:unstructured");
        final Node imageNode2 = root.addNode("image2.jpg", "nt:unstructured");

        addChildFacetNode("image1.jpg", imageNode1.getIdentifier());
        addChildFacetNode("image2.jpg", imageNode2.getIdentifier());

        final Tag image= createImage("/binaries/image1.jpg/{_document}/hippogallery:thumbnail");
        image.addAttribute("data-uuid", imageNode1.getIdentifier());
        image.addAttribute("data-type", "hippogallery:thumbnail");

        write(image);
        assertImage(image, "image1.jpg/{_document}/hippogallery:thumbnail");

        final NodeIterator children = document.getNodes();
        assertEquals("Document node should have only one facet child node", 1, children.getSize());

        final Node child = children.nextNode();
        assertEquals(imageNode1.getIdentifier(), child.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
    }

    private void read(final Tag imageTag) throws RepositoryException {
        read(imageTag, prefixingImageUrlProvider);
    }

    private void read(final Tag image, final UrlProvider urlProvider) throws RepositoryException {
        final ImageVisitor visitor = new ImageVisitor(documentModel, urlProvider);
        visitor.visitBeforeRead(null, image);
    }

    private void write(final Tag imageTag) throws RepositoryException {
        write(imageTag, prefixingImageUrlProvider);
    }

    private void write(final Tag imageTag, final UrlProvider urlProvider) throws RepositoryException {
        final ImageVisitor visitor = new ImageVisitor(documentModel, urlProvider);
        visitor.visitBeforeWrite(null, imageTag);
    }

    private Tag createImage(final String imageSrc) {
        final Tag image = new TestTag("img");
        image.addAttribute("src", imageSrc);
        return image;
    }

    private void assertImage(final Tag image, final String src) {
        assertEquals("img", image.getName());
        assertEquals(src, image.getAttribute("src"));
        assertNull(image.getAttribute("data-uuid"));
        assertNull(image.getAttribute("data-type"));
    }

    private void assertImage(final Tag image, final String src, final String uuid) {
        assertEquals("img", image.getName());
        assertEquals(src, image.getAttribute("src"));
        assertEquals(uuid, image.getAttribute("data-uuid"));
        assertNull(image.getAttribute("data-type"));
    }

    private void assertImage(final Tag image, final String src, final String uuid, final String type) {
        assertEquals("img", image.getName());
        assertEquals(src, image.getAttribute("src"));
        assertEquals(uuid, image.getAttribute("data-uuid"));
        assertEquals(type, image.getAttribute("data-type"));
    }

    private void assertNoChanges(final String imageSrc) throws RepositoryException {
        assertNoChangesReading(imageSrc, src -> src);
        assertNoChangesWriting(imageSrc, src -> src);
    }

    private void assertNoChangesReading(final String src, final UrlProvider urlProvider) throws RepositoryException {
        final Tag image = createImage(src);

        final long childNodesBeforeRead = document.getNodes().getSize();
        read(image, urlProvider);
        assertEquals("Value of src attribute should not have changed during read", src, image.getAttribute("src"));
        assertEquals("Number of child facet nodes should not have changed during read",
                     childNodesBeforeRead, document.getNodes().getSize());
    }

    private void assertNoChangesWriting(final String src, final UrlProvider urlProvider) throws RepositoryException {
        final Tag image = createImage(src);

        final long childNodesBeforeWrite = document.getNodes().getSize();
        write(image, urlProvider);
        assertEquals("Value of src attribute should not have changed during write", src, image.getAttribute("src"));
        assertEquals("Number of child facet nodes should not have changed during write",
                     childNodesBeforeWrite, document.getNodes().getSize());
    }

    private void addChildFacetNode(final String name, final String uuid) throws RepositoryException {
        final Node child = document.addNode(name, HippoNodeType.NT_FACETSELECT);
        child.setProperty(HippoNodeType.HIPPO_DOCBASE, uuid);
    }

    private class PrefixingImageUrlProvider implements UrlProvider {

        private String prefix;

        PrefixingImageUrlProvider(final String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String getURL(final String link) {
            return prefix + "/" + link;
        }
    }

}
