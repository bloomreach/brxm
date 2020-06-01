/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

import org.apache.commons.io.IOUtils;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;

public abstract class AbstractImageOperation implements ImageOperation {

    private ImageOperationResult result;

    public AbstractImageOperation() {
        result = new ImageOperationResult(null, 0, 0);
    }

    static File writeToTmpFile(final InputStream data) throws IOException {
        final File tmpFile = File.createTempFile("hippo-image", ".tmp");
        tmpFile.deleteOnExit();
        OutputStream tmpStream = null;
        try {
            tmpStream = new BufferedOutputStream(new FileOutputStream(tmpFile));
            IOUtils.copy(data, tmpStream);
        } finally {
            IOUtils.closeQuietly(tmpStream);
        }
        return tmpFile;
    }

    /**
     * Executes an image operation.
     *
     * @deprecated Use {@link #run(InputStream, String)} instead
     */
    @Deprecated
    public void execute(final InputStream data, final String mimeType) throws GalleryException {
        run(data, mimeType);
    }

    @Override
    public ImageOperationResult run(final InputStream data, final String mimeType) throws GalleryException {
        ImageReader reader = null;
        ImageWriter writer = null;
        try {
            reader = ImageUtils.getImageReader(mimeType);
            if (reader == null) {
                throw new GalleryException("Unsupported MIME type for reading: " + mimeType);
            }
            writer = ImageUtils.getImageWriter(mimeType);
            if (writer == null) {
                throw new GalleryException("Unsupported MIME type for writing: " + mimeType);
            }
            execute(data, reader, writer);
        } catch (IOException e) {
            throw new GalleryException("Could not execute image operation", e);
        } finally {
            // the != null checks are unnecessary, but otherwise Sonar will report critical issues
            if (reader != null) {
                reader.dispose();
            }
            if (writer != null) {
                writer.dispose();
            }
            IOUtils.closeQuietly(data);
        }

        return result;
    }

    protected void setResult(final InputStream data, final int width, final int height) {
        result = new ImageOperationResult(data, width, height);
    }

    protected ImageOperationResult getResult() {
        return result;
    }

    /**
     * Executes a concrete image operation.
     *
     * @param data the image data. Closing the stream is the responsibility of the caller (i.e. this class).
     * @param reader reader for the image data
     * @param writer writer for the image data
     *
     * @throws IOException when the image operation fails
     */
    public abstract void execute(InputStream data, ImageReader reader, ImageWriter writer) throws IOException;

}
