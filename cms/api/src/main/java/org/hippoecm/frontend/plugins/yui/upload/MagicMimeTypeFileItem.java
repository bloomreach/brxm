/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.wicket.util.upload.FileItem;
import org.onehippo.repository.tika.TikaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagicMimeTypeFileItem implements FileItem {

    private static final Logger log = LoggerFactory.getLogger(MagicMimeTypeFileItem.class);
    private static final Tika tika = TikaFactory.newTika();

    private FileItem delegate;
    private String contentType;

    public MagicMimeTypeFileItem(FileItem delegate) {
        this.delegate = delegate;
    }

    /**
     * @return The best matching mimetype for this fileItem
     */
    private String resolveMimeType(FileItem fileItem) {
        try (InputStream in = fileItem.getInputStream()) {
            final String fileName = fileItem.getName();
            final String extensionBasedMediaType = tika.detect(fileName);
            if (MediaTypeRegistry.getDefaultRegistry().isInstanceOf(extensionBasedMediaType, MediaType.TEXT_PLAIN)) {
                return extensionBasedMediaType;
            }

            String resolvedMediaType = tika.detect(in, fileName);
            if (MediaTypeRegistry.getDefaultRegistry().isInstanceOf(resolvedMediaType, MediaType.APPLICATION_ZIP)) {
                return extensionBasedMediaType;
            }

            return resolvedMediaType;
        } catch (IOException e) {
            log.warn("Tika failed to detect mime-type, falling back on browser provided mime-type", e);
        }
        return fileItem.getContentType();
    }

    public String getContentType() {
        if (contentType == null) {
            contentType = resolveMimeType(delegate);
        }
        return contentType;
    }

    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    public String getName() {
        return delegate.getName();
    }

    public boolean isInMemory() {
        return delegate.isInMemory();
    }

    public long getSize() {
        return delegate.getSize();
    }

    public byte[] get() {
        return delegate.get();
    }

    public String getString(String encoding) throws UnsupportedEncodingException {
        return delegate.getString(encoding);
    }

    public String getString() {
        return delegate.getString();
    }

    public void write(File file) throws IOException {
        delegate.write(file);
    }

    public void delete() {
        delegate.delete();
    }

    public String getFieldName() {
        return delegate.getFieldName();
    }

    public void setFieldName(String name) {
        delegate.setFieldName(name);
    }

    public boolean isFormField() {
        return delegate.isFormField();
    }

    public void setFormField(boolean state) {
        delegate.setFormField(state);
    }

    public OutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }
}
