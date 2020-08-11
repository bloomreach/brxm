/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.vault;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultBundleRootContentXmlEntry implements Archive.Entry {

    private static final Logger log = LoggerFactory.getLogger(DefaultBundleRootContentXmlEntry.class);

    private static final String RESOURCE = "/default-bundle-root-content.xml";
    private static final DefaultBundleRootContentXmlEntry INSTANCE = new DefaultBundleRootContentXmlEntry();

    private DefaultBundleRootContentXmlEntry() {
    }

    public static DefaultBundleRootContentXmlEntry getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return Constants.DOT_CONTENT_XML;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public java.util.Collection<? extends Archive.Entry> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public Archive.Entry getChild(final String name) {
        log.debug("the default bundle root .content.xml does not have children");
        return null;
    }

    InputStream openInputStream() {
        return getClass().getResourceAsStream(RESOURCE);
    }

    VaultInputSource getInputSource() {
        return new VaultInputSource() {

            {
                setSystemId(Constants.DOT_CONTENT_XML);
            }

            public InputStream getByteStream() {
                return DefaultBundleRootContentXmlEntry.this.getClass().getResourceAsStream(RESOURCE);
            }

            @Override
            public long getContentLength() {
                final URL resource = getClass().getResource(RESOURCE);
                final File contentXml = FileUtils.toFile(resource);
                return FileUtils.sizeOf(contentXml);
            }

            @Override
            public long getLastModified() {
                final URL resource = getClass().getResource(RESOURCE);
                final File contentXml = FileUtils.toFile(resource);
                return contentXml.lastModified();
            }
        };
    }
};
