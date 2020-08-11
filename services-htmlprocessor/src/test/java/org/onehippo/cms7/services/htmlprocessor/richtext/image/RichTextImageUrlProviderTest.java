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

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.RichTextException;
import org.onehippo.cms7.services.htmlprocessor.richtext.TestUtil;
import org.onehippo.cms7.services.htmlprocessor.richtext.URLEncoder;
import org.onehippo.cms7.services.htmlprocessor.richtext.jcr.JcrNodeFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.link.RichTextLinkFactory;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RichTextImageUrlProviderTest {

    private MockNode document;
    private MockNode image;
    private Model<Node> documentModel;

    @Before
    public void setUp() throws Exception {
        final MockNode root = MockNode.root();
        document = root.addNode("document", "hippo:document");

        final MockNode path = root.addNode("path", "nt:folder");
        image = path.addNode("image.jpg", "nt:unstructured");

        final JcrNodeFactory nodeFactory = JcrNodeFactory.of(root);
        documentModel = nodeFactory.getNodeModelByNode(document);
    }

    @Test
    public void testEmptyUrl() throws Exception {
        final RichTextImageFactory mockImageFactory = EasyMock.createMock(RichTextImageFactory.class);
        final RichTextLinkFactory mockLinkFactory = EasyMock.createMock(RichTextLinkFactory.class);

        replay(mockImageFactory, mockLinkFactory);

        final RichTextImageURLProvider provider = new RichTextImageURLProvider(mockImageFactory, mockLinkFactory,
                                                                               documentModel);
        assertNull(provider.getURL(null));
        assertEquals("", provider.getURL(""));

        verify(mockImageFactory, mockLinkFactory);
    }

    @Test
    public void testNormalImages() throws Exception {
        final RichTextImageFactory mockImageFactory = EasyMock.createMock(RichTextImageFactory.class);
        final RichTextLinkFactory mockLinkFactory = EasyMock.createMock(RichTextLinkFactory.class);

        replay(mockImageFactory, mockLinkFactory);

        final RichTextImageURLProvider provider = new RichTextImageURLProvider(mockImageFactory, mockLinkFactory,
                                                                               documentModel);

        assertEquals("http://www.test.com/image.jpg", provider.getURL("http://www.test.com/image.jpg"));
        assertEquals("/image.jpg", provider.getURL("/image.jpg"));
        assertEquals("image.jpg", provider.getURL("image.jpg"));

        verify(mockImageFactory, mockLinkFactory);
    }

    @Test
    public void testLinkedImage() throws Exception {
        TestUtil.addChildFacetNode(document, "image.jpg", image.getIdentifier());

        final RichTextImage richTextImage = new RichTextImage("/path/image.jpg/image.jpg", "image.jpg", URLEncoder.OPAQUE);
        richTextImage.setSelectedResourceDefinition("hippogallery:original");

        final RichTextImageFactory mockImageFactory = EasyMock.createMock(RichTextImageFactory.class);
        expect(mockImageFactory.loadImageItem(eq(image.getIdentifier()), eq("hippogallery:original")))
                .andReturn(richTextImage);

        final RichTextLinkFactory mockLinkFactory = EasyMock.createMock(RichTextLinkFactory.class);
        expect(mockLinkFactory.hasLink(eq(image.getIdentifier()))).andReturn(true);

        replay(mockImageFactory, mockLinkFactory);

        final RichTextImageURLProvider provider = new RichTextImageURLProvider(mockImageFactory, mockLinkFactory,
                                                                               documentModel);

        assertEquals("binaries/path/image.jpg/image.jpg/hippogallery:original",
                     provider.getURL("image.jpg/{_document}/hippogallery:original"));

        verify(mockImageFactory, mockLinkFactory);
    }

    @Test
    public void testFacetNotFound() throws Exception {
        TestUtil.addChildFacetNode(document, "image.jpg", image.getIdentifier());

        final RichTextImageFactory mockImageFactory = EasyMock.createMock(RichTextImageFactory.class);
        final RichTextLinkFactory mockLinkFactory = EasyMock.createMock(RichTextLinkFactory.class);

        replay(mockImageFactory, mockLinkFactory);

        final RichTextImageURLProvider provider = new RichTextImageURLProvider(mockImageFactory, mockLinkFactory,
                                                                               documentModel);

        assertEquals("non-existing-image.jpg/{_document}/hippogallery:original",
                     provider.getURL("non-existing-image.jpg/{_document}/hippogallery:original"));

        verify(mockImageFactory, mockLinkFactory);
    }

    @Test
    public void testUuidNotFound() throws Exception {
        TestUtil.addChildFacetNode(document, "image.jpg", image.getIdentifier());

        final RichTextImageFactory mockImageFactory = EasyMock.createMock(RichTextImageFactory.class);
        final RichTextLinkFactory mockLinkFactory = EasyMock.createMock(RichTextLinkFactory.class);
        expect(mockLinkFactory.hasLink(eq(image.getIdentifier()))).andReturn(false);

        replay(mockImageFactory, mockLinkFactory);

        final RichTextImageURLProvider provider = new RichTextImageURLProvider(mockImageFactory, mockLinkFactory,
                                                                               documentModel);

        assertEquals("image.jpg/{_document}/hippogallery:original",
                     provider.getURL("image.jpg/{_document}/hippogallery:original"));

        verify(mockImageFactory, mockLinkFactory);
    }

    @Test
    public void testErrorLoadingImageReturnsInlineBase64Image() throws Exception {
        TestUtil.addChildFacetNode(document, "image.jpg", image.getIdentifier());

        final RichTextImageFactory mockImageFactory = EasyMock.createMock(RichTextImageFactory.class);
        expect(mockImageFactory.loadImageItem(eq(image.getIdentifier()), eq("hippogallery:original")))
                .andThrow(new RichTextException("Expected exception"));

        final RichTextLinkFactory mockLinkFactory = EasyMock.createMock(RichTextLinkFactory.class);
        expect(mockLinkFactory.hasLink(eq(image.getIdentifier()))).andReturn(true);

        replay(mockImageFactory, mockLinkFactory);

        final RichTextImageURLProvider provider = new RichTextImageURLProvider(mockImageFactory, mockLinkFactory,
                                                                               documentModel);

        final String url = provider.getURL("image.jpg/{_document}/hippogallery:original");
        assertTrue(StringUtils.startsWith(url, "data:image/"));
        assertTrue(StringUtils.contains(url, ";base64"));

        verify(mockImageFactory, mockLinkFactory);
    }
}
