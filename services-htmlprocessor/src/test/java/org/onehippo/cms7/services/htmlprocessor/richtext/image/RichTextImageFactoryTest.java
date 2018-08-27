/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.richtext.image;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.easymock.EasyMock;
import org.hamcrest.CoreMatchers;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.RichTextException;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.JcrNodeFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.NodeFactory;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RichTextImageFactoryTest {

    private MockNode root;
    private Node htmlNode;
    private NodeFactory nodeFactory;
    private MockNode imagesFolder;

    @Before
    public void setUp() throws RepositoryException {
        root = MockNode.root();

        final Node sourceHandle = root.addNode("source", "hippo:handle");
        htmlNode = sourceHandle.addNode("source", "richtexttest:testdocument").addNode("richtexttest:html", "hippostd:html");
        htmlNode.setProperty("hippostd:content", "testing 1 2 3");

        imagesFolder = root.addNode("images", "nt:folder");

        nodeFactory = JcrNodeFactory.of(root);
    }

    @Test
    public void testCreateImages() throws Exception {
        final RichTextImageFactoryImpl factory = getJcrRichTextImageFactory();
        final Model<Node> imageModel1 = createImageModel("image1.jpg");
        final Model<Node> imageModel2 = createImageModel("image2.jpg");

        final RichTextImage image1 = factory.createImageItem(imageModel1);
        assertRichTextImage(image1, "image1.jpg");

        final RichTextImage image2 = factory.createImageItem(imageModel2);
        assertRichTextImage(image2, "image2.jpg");
    }

    @Test
    public void testCreateDuplicateImages() throws Exception {
        final RichTextImageFactoryImpl factory = getJcrRichTextImageFactory();
        final Model<Node> imageModel = createImageModel("image.jpg");

        final RichTextImage image1 = factory.createImageItem(imageModel);
        final RichTextImage image2 = factory.createImageItem(imageModel);

        assertEquals(1, htmlNode.getNodes("image.jpg").getSize());
        assertRichTextImage(image1, "image.jpg");

        assertEquals(1, htmlNode.getNodes("image.jpg_1").getSize());
        assertRichTextImage(image2, "image.jpg", "image.jpg_1");
    }

    @Test
    public void testThrowsIfImageIsInvalid() throws Exception {
        final RichTextImageFactoryImpl factory = getJcrRichTextImageFactory();
        final MockNode imageHandle = imagesFolder.addNode("image.jpg", "hippo:handle");
        final MockNode imageNode = imageHandle.addNode("image.jpg", "hippogallery:imageset");
        imageNode.setPrimaryItemName("not-an-image");
        imageNode.addNode("not-an-image", "nt:unstructured");
        try {
            factory.createImageItem(nodeFactory.getNodeModelByNode(imageHandle));
            fail("Should throw an exception");
        } catch (final RichTextException e) {
            assertEquals("Invalid image document", e.getMessage());
        }
    }

    @Test
    public void testIsValid() throws Exception {
        final RichTextImageFactoryImpl factory = getJcrRichTextImageFactory();

        assertFalse(factory.isValid(null));
        assertFalse(factory.isValid(Model.of(null)));

        final Node noHandle = root.addNode("not-a-handle", "nt:unstructured");
        assertFalse(factory.isValid(nodeFactory.getNodeModelByNode(noHandle)));

        final Node handleWithoutChild = root.addNode("handle", HippoNodeType.NT_HANDLE);
        assertFalse(factory.isValid(nodeFactory.getNodeModelByNode(handleWithoutChild)));

        final MockNode handleWithoutSameNameChild = root.addNode("handle", HippoNodeType.NT_HANDLE);
        handleWithoutSameNameChild.addNode("not-a-handle", HippoNodeType.NT_RESOURCE);
        assertFalse(factory.isValid(nodeFactory.getNodeModelByNode(handleWithoutSameNameChild)));

        final MockNode handleWithWrongChild = root.addNode("handle", HippoNodeType.NT_HANDLE);
        handleWithoutSameNameChild.addNode("handle", "nt:unstructured");
        assertFalse(factory.isValid(nodeFactory.getNodeModelByNode(handleWithWrongChild)));

        final Node mockNode = EasyMock.createMock(Node.class);
        expect(mockNode.getIdentifier()).andReturn("broken-node-uuid");
        replay(mockNode);
        assertFalse(factory.isValid(nodeFactory.getNodeModelByNode(mockNode)));

        final MockNode handleWithResource = root.addNode("handle", HippoNodeType.NT_HANDLE);
        final MockNode document = handleWithResource.addNode("handle", HippoNodeType.NT_DOCUMENT);
        document.addNode("image-resource-node", HippoNodeType.NT_RESOURCE);
        document.setPrimaryItemName("image-resource-node");
        assertTrue(factory.isValid(nodeFactory.getNodeModelByNode(handleWithResource)));

        EasyMock.verify(mockNode);
    }

    @Test
    public void testLoadImage() throws Exception {
        final RichTextImageFactoryImpl factory = getJcrRichTextImageFactory();
        final Model<Node> imageModel = createImageModel("image1.jpg");

        factory.createImageItem(imageModel);
        final Node imageNode = imageModel.get();

        final RichTextImage imageLoaded = factory.loadImageItem(imageNode.getIdentifier(), null);
        assertEquals("/images/image1.jpg/image1.jpg", imageLoaded.getPath());
        assertEquals(imageNode.getIdentifier(), imageLoaded.getUuid());
    }

    private void assertRichTextImage(final RichTextImage richTextImage, final String name) throws RepositoryException {
        assertRichTextImage(richTextImage, name, name);
    }

    private void assertRichTextImage(final RichTextImage richTextImage, final String name, final String facet) throws RepositoryException {
        assertTrue(htmlNode.hasNode(facet));
        assertEquals(HippoNodeType.NT_FACETSELECT, htmlNode.getNode(facet).getPrimaryNodeType().getName());
        assertEquals(facet, richTextImage.getName());
        assertEquals("/images/" + name + "/" + name, richTextImage.getPath());
        // FIXME: hippogallery:thumbnail is the primaryType which ensures the second resourceDefinition (hippogallery:original) is used here. Seems strange.
        assertEquals(facet + "/{_document}/hippogallery:original", richTextImage.getFacetSelectPath());
        assertEquals("hippogallery:original", richTextImage.getSelectedResourceDefinition());
        assertThat(richTextImage.getResourceDefinitions(), CoreMatchers.hasItems("hippogallery:thumbnail", "hippogallery:original"));
        assertEquals(root.getNode("images/" + name).getIdentifier(), richTextImage.getUuid());
    }

    private RichTextImageFactoryImpl getJcrRichTextImageFactory() throws RepositoryException {
        return new RichTextImageFactoryImpl(nodeFactory.getNodeModelByNode(htmlNode), nodeFactory);
    }

    private Model<Node> createImageModel(final String name) throws RepositoryException {
        final MockNode imageHandle = imagesFolder.addNode(name, "hippo:handle");
        final MockNode imageNode = imageHandle.addNode(name, "hippogallery:imageset");
        imageNode.setPrimaryItemName("hippogallery:thumbnail");
        imageNode.addNode("hippogallery:thumbnail", "hippo:resource");
        imageNode.addNode("hippogallery:original", "hippo:resource");
        return nodeFactory.getNodeModelByNode(imageHandle);
    }
}
