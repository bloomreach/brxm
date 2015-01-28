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

import java.io.IOException;
import java.io.InputStream;

import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.onehippo.cms7.services.webfiles.WebFileException;

/**
 * Decorates an {@link Archive} that contains a complete web file bundle. All web files are read from the
 * first root directory in the decorated archive. When that bundle root directory (which is returned by
 * {@link AbstractWebFilesArchive#getBundleRoot()}) does not contain a .content.xml file, this decorated
 * archive will automatically include a default .content.xml file.
 *
 * @see WebFileBundleJcrRootEntry
 * @see WebFileBundleRootEntry
 */
public class WebFileBundleArchive implements Archive {

    private final AbstractWebFilesArchive decorated;

    public WebFileBundleArchive(final AbstractWebFilesArchive decorated) {
        this.decorated = decorated;
    }

    @Override
    public void open(final boolean strict) throws IOException {
        decorated.open(strict);
    }

    public String getBundleName() throws WebFileException {
        final Archive.Entry bundleRoot = decorated.getBundleRoot();
        if (bundleRoot == null) {
            throw new WebFileException("Cannot determine the web file bundle name: the archive contains no directories");
        }
        return bundleRoot.getName();
    }

    @Override
    public MetaInf getMetaInf() {
        return decorated.getMetaInf();
    }

    @Override
    public Entry getRoot() throws IOException {
        return decorated.getRoot();
    }

    @Override
    public Entry getJcrRoot() throws IOException {
        return new WebFileBundleJcrRootEntry(decorated.getJcrRoot());
    }

    @Override
    public Archive getSubArchive(final String root, final boolean asJcrRoot) throws IOException {
        return decorated.getSubArchive(root, asJcrRoot);
    }

    @Override
    public Entry getEntry(final String path) throws IOException {
        final Entry entry = decorated.getEntry(path);
        if (entry == decorated.getBundleRoot()) {
            return new WebFileBundleRootEntry(entry);
        }
        return entry;
    }

    @Override
    public InputStream openInputStream(final Entry entry) throws IOException {
        if (entry == DefaultBundleRootContentXmlEntry.getInstance()) {
            return DefaultBundleRootContentXmlEntry.getInstance().openInputStream();
        }
        return decorated.openInputStream(entry);
    }

    @Override
    public VaultInputSource getInputSource(final Entry entry) throws IOException {
        if (entry == DefaultBundleRootContentXmlEntry.getInstance()) {
            return DefaultBundleRootContentXmlEntry.getInstance().getInputSource();
        }
        return decorated.getInputSource(entry);
    }

    @Override
    public void close() {
        decorated.close();
    }

}
