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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.crisp.api.resource.Binary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Spring Framework {@link Resource} based {@link Binary} implementation.
 */
public class SpringResourceBinary implements Binary {

    private static final long serialVersionUID = 1L;

    private final Resource resource;
    private final boolean temporaryResource;
    private InputStream inputStream;

    public SpringResourceBinary(final Resource resource, final boolean temporaryResource) throws IOException {
        if (resource == null) {
            throw new IllegalArgumentException("resource is null.");
        }

        this.resource = resource;
        this.temporaryResource = temporaryResource;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream != null) {
            IOUtils.closeQuietly(inputStream);
        }

        if (!resource.exists()) {
            throw new IOException("resource doesn't exist.");
        }

        if (!resource.isReadable()) {
            throw new IOException("resource isn't readable.");
        }

        inputStream = resource.getInputStream();

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

        if (temporaryResource) {
            if (resource instanceof FileSystemResource) {
                ((FileSystemResource) resource).getFile().delete();
            }
        }
    }
}
