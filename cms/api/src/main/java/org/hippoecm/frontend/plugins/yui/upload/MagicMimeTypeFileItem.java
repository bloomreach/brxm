/*
 *  Copyright 2010-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.commons.fileupload.FileItem;
import org.hippoecm.frontend.editor.plugins.resource.InvalidFileNameException;
import org.hippoecm.frontend.editor.plugins.resource.InvalidMimeTypeException;
import org.onehippo.repository.tika.TikaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagicMimeTypeFileItem implements FileItem {

    private static final Logger log = LoggerFactory.getLogger(MagicMimeTypeFileItem.class);
    private static final Tika tika = TikaFactory.newTika();

    private final FileItem delegate;
    private String tikaDetectedContentType;

    public static ThreadLocal<Boolean> mimetypeValidationContext = new ThreadLocal<>();

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

    /**
     * <p>
     *     Returns the content type (mime type) for this file item. In case the browser provided mimetype does not match
     *     the content detected mimetype, eg when a .exe is renamed to a .pdf, an {@link InvalidMimeTypeException}
     *     will be thrown
     * </p>
     * @return the content type for the file
     * @throws InvalidMimeTypeException if the content type from the browser upload is different than
     * the actual detected type of the content via Tika
     */
    public String getContentType() {
        if (tikaDetectedContentType == null) {
            tikaDetectedContentType = resolveMimeType(delegate);
        }
        if (mimetypeValidationContext.get() == null || !mimetypeValidationContext.get().booleanValue()) {
            log.debug("No mimetype validation against browser provide mimetype check needed");
            return tikaDetectedContentType;
        }
        log.debug("Mimetype validation against browser provide mimetype check needed");
        if (tikaDetectedContentType == null) {
            throw new InvalidMimeTypeException("Could not detect mimetype from content", "unknown");
        }
        // by default, executable files are not allowed
        if (tikaDetectedContentType.equalsIgnoreCase("application/x-dosexec")) {
            log.debug("Detected executable. Only if the extension is .exe and .exe is explicitly allowed as content type," +
                    "the upload is allowed. Otherwise, an InvalidMimeTypeException will be thrown");
            throw new InvalidMimeTypeException("Executable file upload not allowed", tikaDetectedContentType);
        }
        if (!tikaDetectedContentType.equalsIgnoreCase(delegate.getContentType())) {
            // it might be that Tika returns a more specific child contentType or super contentType than the one provided
            // by the browser. We now have to validate this and if this is the case, the mimetype check still passes
            if (MediaTypeRegistry.getDefaultRegistry().isInstanceOf(delegate.getContentType(), MediaType.parse(tikaDetectedContentType))) {
                log.debug("Browser provided content type '{}' is a subtype of '{}'", delegate.getContentType(), tikaDetectedContentType);
            } else if (MediaTypeRegistry.getDefaultRegistry().isInstanceOf(tikaDetectedContentType, MediaType.parse(delegate.getContentType()))) {
                log.debug("Browser provided content type '{}' is a super type of '{}'", delegate.getContentType(), tikaDetectedContentType);
            } else {
                log.debug("Detected mimetype by Tika '{}' does not match the provided mimetype by the browser '{}'", tikaDetectedContentType, delegate.getContentType());
                throw new InvalidMimeTypeException(String.format("Could not detect mimetype or detected mimetype different than " +
                        "request mimetype '%s'. Tika detected mimetype was '%s'", delegate.getContentType(), tikaDetectedContentType),
                        tikaDetectedContentType);
            }
        }
        return tikaDetectedContentType;
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

    public void write(File file) throws Exception {
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

    public FileItemHeaders getHeaders() {
        return delegate.getHeaders();
    }

    public void setHeaders(FileItemHeaders headers) {
        delegate.setHeaders(headers);
    }
}
