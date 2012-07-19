/*
 * Copyright 2012 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ImageMetadataTest extends AbstractImageTest {

    @Test
    public void testMetadataForRGBJpeg() throws Exception {

        ImageMetadata meta = new ImageMetadata(JPEG_MIME_TYPE, TEST_RGB_JPG);
        org.junit.Assert.assertEquals(TEST_RGB_JPG, meta.getFilename());
        org.junit.Assert.assertEquals(JPEG_MIME_TYPE, meta.getMimetype());
        org.junit.Assert.assertTrue(meta.isJpeg());

        InputStream is = meta.parse(getRGBJpeg());
        org.junit.Assert.assertEquals(TEST_RGB_JPG, meta.getFilename());
        org.junit.Assert.assertEquals(JPEG_MIME_TYPE, meta.getMimetype());
        org.junit.Assert.assertTrue(meta.isJpeg());
        org.junit.Assert.assertFalse(meta.isUnknown());
        org.junit.Assert.assertTrue(meta.isRGB());
        org.junit.Assert.assertFalse(meta.isCMYK());
        org.junit.Assert.assertFalse(meta.isYCCK());

        IOUtils.closeQuietly(is);
    }
    @Test
    public void testMetadataForYCCKJpeg() throws Exception {

        ImageMetadata meta = new ImageMetadata(JPEG_MIME_TYPE, TEST_YCCK_JPG);
        org.junit.Assert.assertEquals(TEST_YCCK_JPG, meta.getFilename());
        org.junit.Assert.assertEquals(JPEG_MIME_TYPE, meta.getMimetype());
        org.junit.Assert.assertTrue(meta.isJpeg());

        InputStream is = meta.parse(getYCCKJpeg());
        org.junit.Assert.assertEquals(TEST_YCCK_JPG, meta.getFilename());
        org.junit.Assert.assertEquals(JPEG_MIME_TYPE, meta.getMimetype());
        org.junit.Assert.assertTrue(meta.isJpeg());
        org.junit.Assert.assertFalse(meta.isUnknown());
        org.junit.Assert.assertFalse(meta.isRGB());
        org.junit.Assert.assertFalse(meta.isCMYK());
        org.junit.Assert.assertTrue(meta.isYCCK());

        IOUtils.closeQuietly(is);
    }

    @Test
    public void testMetadataForCMYKJpeg() throws Exception {

        ImageMetadata meta = new ImageMetadata(JPEG_MIME_TYPE, TEST_CMYK_JPG);
        org.junit.Assert.assertEquals(TEST_CMYK_JPG, meta.getFilename());
        org.junit.Assert.assertEquals(JPEG_MIME_TYPE, meta.getMimetype());
        org.junit.Assert.assertTrue(meta.isJpeg());

        InputStream is = meta.parse(getCMYKJpeg());
        org.junit.Assert.assertEquals(TEST_CMYK_JPG, meta.getFilename());
        org.junit.Assert.assertEquals(JPEG_MIME_TYPE, meta.getMimetype());
        org.junit.Assert.assertTrue(meta.isJpeg());
        org.junit.Assert.assertFalse(meta.isUnknown());
        org.junit.Assert.assertFalse(meta.isRGB());
        org.junit.Assert.assertTrue(meta.isCMYK());
        org.junit.Assert.assertFalse(meta.isYCCK());

        IOUtils.closeQuietly(is);
    }

    @Test
    public void testMetadataForRGBPng() throws Exception {

        ImageMetadata meta = new ImageMetadata(PNG_MIME_TYPE, TEST_RGB_PNG);
        org.junit.Assert.assertEquals(TEST_RGB_PNG, meta.getFilename());
        org.junit.Assert.assertEquals(PNG_MIME_TYPE, meta.getMimetype());
        org.junit.Assert.assertFalse(meta.isJpeg());

        InputStream is = meta.parse(getRGBPng());
        org.junit.Assert.assertEquals(TEST_RGB_PNG, meta.getFilename());
        org.junit.Assert.assertEquals(PNG_MIME_TYPE, meta.getMimetype());
        org.junit.Assert.assertFalse(meta.isJpeg());
        org.junit.Assert.assertFalse(meta.isUnknown());
        org.junit.Assert.assertTrue(meta.isRGB());
        org.junit.Assert.assertFalse(meta.isCMYK());
        org.junit.Assert.assertFalse(meta.isYCCK());

        IOUtils.closeQuietly(is);
    }

    @Test
    public void testMetadataForRGBGif() throws Exception {

        ImageMetadata meta = new ImageMetadata(GIF_MIME_TYPE, TEST_RGB_GIF);
        org.junit.Assert.assertEquals(TEST_RGB_GIF, meta.getFilename());
        org.junit.Assert.assertEquals(GIF_MIME_TYPE, meta.getMimetype());
        org.junit.Assert.assertFalse(meta.isJpeg());

        InputStream is = meta.parse(getRGBGif());
        org.junit.Assert.assertEquals(TEST_RGB_GIF, meta.getFilename());
        org.junit.Assert.assertEquals(GIF_MIME_TYPE, meta.getMimetype());
        org.junit.Assert.assertFalse(meta.isJpeg());
        org.junit.Assert.assertFalse(meta.isUnknown());
        org.junit.Assert.assertTrue(meta.isRGB());
        org.junit.Assert.assertFalse(meta.isCMYK());
        org.junit.Assert.assertFalse(meta.isYCCK());

        IOUtils.closeQuietly(is);
    }

}
