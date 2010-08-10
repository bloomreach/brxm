/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.gallery.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class ResourceHelper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static final String MIME_IMAGE_PJPEG = "image/pjpeg";
    public static final String MIME_IMAGE_JPEG = "image/jpeg";

    private ResourceHelper() {
    }

    public static void validateResource(Node resource, String filename) throws GalleryException, RepositoryException {
        try {
            String mimeType = (resource.hasProperty("jcr:mimeType") ? resource.getProperty("jcr:mimeType").getString()
                    : "");
            mimeType = mimeType.toLowerCase();
            if (mimeType.equals(MIME_IMAGE_PJPEG)) {
                mimeType = MIME_IMAGE_JPEG;
            }
            if (mimeType.startsWith("image/")) {
                ImageInfo imageInfo = new ImageInfo();
                imageInfo.setInput(resource.getProperty("jcr:data").getStream());
                if (imageInfo.check()) {
                    String imageInfoMimeType = imageInfo.getMimeType();
                    if (imageInfoMimeType == null) {
                        throw new GalleryException("impermissable image type content");
                    } else {
                        if (imageInfoMimeType.equals(MIME_IMAGE_PJPEG)) {
                            imageInfoMimeType = MIME_IMAGE_JPEG;
                        }
                        if (!imageInfoMimeType.equalsIgnoreCase(mimeType)) {
                            throw new GalleryException("mismatch image mime type");
                        }
                    }
                } else {
                    throw new GalleryException("impermissable image type content");
                }
            } else if (mimeType.equals("application/pdf")) {
                String line;
                line = new BufferedReader(new InputStreamReader(resource.getProperty("jcr:data").getStream()))
                        .readLine().toUpperCase();
                if (!line.startsWith("%PDF-")) {
                    throw new GalleryException("impermissable pdf type content");
                }
            } else if (mimeType.equals("application/postscript")) {
                String line;
                line = new BufferedReader(new InputStreamReader(resource.getProperty("jcr:data").getStream()))
                        .readLine().toUpperCase();
                if (!line.startsWith("%!")) {
                    throw new GalleryException("impermissable postscript type content");
                }
            } else {
                // This method can be overridden to allow more such checks on content type.  if such an override
                // wants to be really strict and not allow unknown content, the following thrown exception is to be included
                // throw new ValueFormatException("impermissable unrecognized type content");
            }
        } catch (IOException ex) {
            throw new GalleryException("impermissable unknown type content");
        }
    }

}
