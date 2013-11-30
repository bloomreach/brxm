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

package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sanselan.ImageReadException;
import org.apache.wicket.util.io.IOUtils;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ImageBinaryTest extends RepositoryTestCase {

    private static final String TEST_RGB_JPG = "test-RGB.jpg";
    private static final String TEST_CMYK_JPG = "test-CMYK.jpg";
    private static final String TEST_YCCK_JPG = "test-YCCK.jpg";
    private static final String TEST_RGB_PNG = "test-5000x1.png";
    private static final String TEST_RGB_GIF = "test-380x428.gif";

    private static final String TEST_BROKEN_THUMBS_JPG = "test-broken-thumbnails.jpg";

    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String PNG_MIME_TYPE = "image/png";
    private static final String GIF_MIME_TYPE = "image/gif";

    private Node imageNode;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
            session.refresh(false);
        }

        String[] content = new String[] {
                "/test", "hippostd:folder",
                "/test/image", "hippo:resource",
                "jcr:data", "",
                "jcr:mimeType", "image/png",
                "jcr:lastModified", "2013-11-29T20:36:24.377+01:00"
        };
        build(session, content);
        session.save();

        imageNode = session.getNode("/test/image");
    }

    private InputStream readImage(String fileName) {
        return getClass().getResourceAsStream("/" + fileName);
    }

    private ImageBinary createImageBinary(String fileName, String mimeType) throws GalleryException {
        return new ImageBinary(imageNode, readImage(fileName), fileName, mimeType);
    }

    //Use mime type auto-detection when creating an image binary like this
    private ImageBinary createImageBinary(final String fileName) throws GalleryException {
        return new ImageBinary(imageNode, readImage(fileName), fileName);
    }

    private void testImage(final String fileName, final String mimeType)
            throws GalleryException, RepositoryException, IOException, ImageReadException {

        ImageBinary binary = createImageBinary(fileName, mimeType);
        assertEquals(fileName, binary.getFileName());
        assertEquals(mimeType, binary.getMimeType());
        assertEquals(ColorModel.RGB, binary.getColorModel());
        assertTrue(ImageUtilTest.isRGB(binary.getStream(), fileName));
        assertEquals(mimeType, createImageBinary(fileName).getMimeType());
    }

    @Test
    public void testObscureMimeTypes() throws Exception {
        assertEquals(GIF_MIME_TYPE, createImageBinary(TEST_RGB_GIF, ResourceHelper.MIME_TYPE_CITRIX_GIF).getMimeType());
        assertEquals(JPEG_MIME_TYPE, createImageBinary(TEST_RGB_JPG, ResourceHelper.MIME_TYPE_CITRIX_JPEG).getMimeType());
        assertEquals(JPEG_MIME_TYPE, createImageBinary(TEST_RGB_JPG, ResourceHelper.MIME_TYPE_PJPEG).getMimeType());
        assertEquals(PNG_MIME_TYPE, createImageBinary(TEST_RGB_PNG, ResourceHelper.MIME_TYPE_X_PNG).getMimeType());
    }

    @Test
    public void testSupportedImages() throws Exception {
        testImage(TEST_RGB_JPG, JPEG_MIME_TYPE);
        testImage(TEST_YCCK_JPG, JPEG_MIME_TYPE);
        testImage(TEST_CMYK_JPG, JPEG_MIME_TYPE);
        testImage(TEST_RGB_PNG, PNG_MIME_TYPE);
        testImage(TEST_RGB_GIF, GIF_MIME_TYPE);
    }

    /**
     * Sometimes images contain broken thumbnails which generates an error when the metadata is parsed. This test
     * verifies that this error is not thrown.
     * See https://issues.apache.org/jira/browse/IMAGING-50?focusedCommentId=13162306&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13162306
     */
    @Test
    public void testBrokenThumbnailsJpg() {
        try {
            createImageBinary(TEST_BROKEN_THUMBS_JPG, JPEG_MIME_TYPE);
        } catch (GalleryException e) {
            fail("GalleryException should not have been thrown");
        }
    }

    @Test(expected = GalleryException.class)
    public void testException() throws GalleryException {
        createImageBinary("test.pdf");
    }

    @Test
    public void testStreams() throws Exception {
        ImageBinary binary = createImageBinary(TEST_RGB_JPG, JPEG_MIME_TYPE);
        assertTrue(IOUtils.contentEquals(readImage(TEST_RGB_JPG), binary.getStream()));

        imageNode.setProperty("jcr:data", binary);
        session.save();

        Binary savedBinary = imageNode.getProperty("jcr:data").getBinary();
        assertTrue(IOUtils.contentEquals(savedBinary.getStream(), binary.getStream()));
    }

}
