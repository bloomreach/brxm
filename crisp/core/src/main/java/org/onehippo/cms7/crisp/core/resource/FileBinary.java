/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.crisp.api.resource.Binary;

/**
 * {@link File} based {@link Binary} implementation.
 * <P>
 * This stores binary input stream into a file. When {@link #dispose()} is called, it closes the input stream and
 * internal file if any.
 * </P>
 */
public class FileBinary implements Binary {

    private static final long serialVersionUID = 1L;

    private File file;
    private InputStream inputStream;

    public FileBinary() throws IOException {
    }

    public void save(final File file, final InputStream input) throws IOException {
        dispose();

        this.file = file;

        FileOutputStream output = null;

        try {
            output = new FileOutputStream(file);
            IOUtils.copy(input, output);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream != null) {
            IOUtils.closeQuietly(inputStream);
        }

        if (file == null || !file.isFile()) {
            throw new IOException("File wasn't saved.");
        }

        inputStream = new FileInputStream(file);
        return inputStream;
    }

    @Override
    public void dispose() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ignore) {
            } finally {
                inputStream = null;
            }
        }

        if (file != null) {
            file.delete();
        }
    }
}
