/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.jquery.upload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.hippoecm.frontend.plugins.yui.upload.MagicMimeTypeFileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of the {@link MagicMimeTypeFileItem} that tries to close all opening stream before deletion. It fixes errors in deleting
 * temporary files on Windows.
 */
public class TemporaryFileItem extends MagicMimeTypeFileItem {
    private static final Logger log = LoggerFactory.getLogger(TemporaryFileItem.class);

    private final List<InputStream> lstOpeningIS = new ArrayList<>();
    private List<OutputStream> lstOpeningOS = new ArrayList<>();

    public TemporaryFileItem(final FileItem delegate) {
        super(delegate);
    }

    @Override
    public void delete() {
        // try to close all input/output stream referenced to the temporary file
        if (!lstOpeningIS.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("{} InputStream(s) of '{}' are opening, trying to close", lstOpeningIS.size(), getName());
            }
            for (InputStream is : lstOpeningIS) {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    log.warn("Error closing an InputStream of the temporary file {}", getName(), e);
                }
            }
            lstOpeningIS.clear();
        }
        if (!lstOpeningOS.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("{} OutputStream(s) of '{}' are opening, trying to close", lstOpeningOS.size(), getName());
            }
            for (OutputStream os : lstOpeningOS) {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                    log.warn("Error closing an OutputStream of the temporary file {}", getName(), e);
                }
            }
            lstOpeningOS.clear();
        }
        super.delete();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        // keep track of opening stream to close when delete temporary file
        InputStream is = super.getInputStream();
        this.lstOpeningIS.add(is);
        return is;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        OutputStream os = super.getOutputStream();
        lstOpeningOS.add(os);
        return os;
    }
}
