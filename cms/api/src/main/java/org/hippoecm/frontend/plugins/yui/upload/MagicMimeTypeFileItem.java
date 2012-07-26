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
package org.hippoecm.frontend.plugins.yui.upload;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;
import org.apache.wicket.util.upload.DiskFileItem;
import org.apache.wicket.util.upload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

public class MagicMimeTypeFileItem implements FileItem {

    private static final Logger log = LoggerFactory.getLogger(MagicMimeTypeFileItem.class);

    private FileItem delegate;
    private String contentType;
    private static final String MAGIC_MIME_DETECTOR = "eu.medsea.mimeutil.detector.MagicMimeMimeDetector";
    private static final String EXTENSIONS_MIME_DETECTOR = "eu.medsea.mimeutil.detector.ExtensionMimeDetector";

    public MagicMimeTypeFileItem(FileItem delegate) {
        this.delegate = delegate;

        if(MimeUtil.getMimeDetector(MAGIC_MIME_DETECTOR) == null) {
            MimeUtil.registerMimeDetector(MAGIC_MIME_DETECTOR);
        }
        if(MimeUtil.getMimeDetector(EXTENSIONS_MIME_DETECTOR) == null) {
            MimeUtil.registerMimeDetector(EXTENSIONS_MIME_DETECTOR);
        }
    }

    /**
     * Microsoft and OpenOffice files aren't correctly detected based on Magic bytes, so fall back on extensions
     * detection for these mimetypes. Also, do a second detection run based on extesion for
     * mimetype=application/octen-stream
     *
     * .odt, .ods and .odp are detected as application/zip
     * .xsl and .ppt are detected as application/msword
     *
     * @param fileItem
     * @return The best matching mimetype for this fileItem
     */
    private String resolveMimeType(FileItem fileItem) {
        Collection<?> mimeTypes = null;
        if(fileItem instanceof DiskFileItem && !fileItem.isInMemory()) {
            mimeTypes = MimeUtil.getMimeTypes(((DiskFileItem)fileItem).getStoreLocation());
        } else {
            InputStream inputStream = null;
            try {
                inputStream = fileItem.getInputStream();
                mimeTypes = MimeUtil.getMimeTypes(new BufferedInputStream(inputStream));
            } catch (IOException e) {
                log.warn("IOException prevented retrieval of mimetype; using default", e);
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    log.warn("Could not close inputstream after retrieving mimetype", e);
                }
            }
        }
        if (mimeTypes != null && mimeTypes.size() == 1) {
            MimeType mimeType = (MimeType) mimeTypes.iterator().next();
            if (mimeType.getMediaType().equals("application")) {
                if(mimeType.getSubType().equals("msword") || mimeType.getSubType().equals("zip") || mimeType.getSubType().equals("octet-stream")) {
                    Collection<?> extensionBasedMimeTypes = MimeUtil.getMimeTypes(fileItem.getName());
                    if(extensionBasedMimeTypes != null && extensionBasedMimeTypes.size() > 0) {
                        mimeTypes = extensionBasedMimeTypes;
                    }
                }
            }
        }
        if(mimeTypes != null && mimeTypes.size() > 0) {
            MimeType mimeType = (MimeType) mimeTypes.iterator().next();
            return mimeType.toString();
        }
        return fileItem.getContentType();
    }

    public String getContentType() {
        if(contentType == null) {
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
}
