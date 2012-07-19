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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class AbstractImageTest {

    public static final String TEST_RGB_JPG     = "test-RGB.jpg";
    public static final String TEST_CMYK_JPG    = "test-CMYK.jpg";
    public static final String TEST_YCCK_JPG    = "test-YCCK.jpg";
    public static final String TEST_RGB_PNG     = "test-5000x1.png";
    public static final String TEST_RGB_GIF     = "test-380x428.gif";

    public static final String JPEG_MIME_TYPE   = "image/jpeg";
    public static final String PNG_MIME_TYPE   = "image/png";
    public static final String GIF_MIME_TYPE   = "image/gif";

    protected InputStream getInputStream(final String fileName) {
        return getClass().getResourceAsStream("/" + fileName);
    }

    public InputStream getRGBJpeg() {
        return getInputStream(TEST_RGB_JPG);
    }

    public InputStream getCMYKJpeg() {
        return getInputStream(TEST_CMYK_JPG);
    }

    public InputStream getYCCKJpeg() {
        return getInputStream(TEST_YCCK_JPG);
    }

    public InputStream getRGBPng() {
        return getInputStream(TEST_RGB_PNG);
    }

    public InputStream getRGBGif() {
        return getInputStream(TEST_RGB_GIF);
    }

    protected static void assertEquals(InputStream i1, InputStream i2) throws IOException {
        org.junit.Assert.assertTrue(isEqual(i1, i2));
    }

    protected static void assertNotEquals(InputStream i1, InputStream i2) throws IOException {
        org.junit.Assert.assertFalse(isEqual(i1, i2));
    }

    protected static boolean isEqual(InputStream i1, InputStream i2)
            throws IOException {
        byte[] buf1 = new byte[64 *1024];
        byte[] buf2 = new byte[64 *1024];
        try {
            DataInputStream d2 = new DataInputStream(i2);
            int len;
            while ((len = i1.read(buf1)) > 0) {
                d2.readFully(buf2,0,len);
                for(int i=0;i<len;i++)
                    if(buf1[i] != buf2[i]) return false;
            }
            return d2.read() < 0; // is the end of the second file also.
        } catch(EOFException ioe) {
            return false;
        } finally {
            i1.close();
            i2.close();
        }
    }
}
