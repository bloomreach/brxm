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
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.config.DefaultMetaInf;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.config.VaultSettings;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.util.Text;
import org.onehippo.cms7.services.webfiles.watch.GlobFileNameMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.services.webfiles.vault.FileNameComparatorUtils.BASE_NAME_COMPARATOR;

/**
 * Filevault archive based on the contents of a zip file. Everything in the zip file is assumed to be content,
 * and there is no meta-data directory read. For example, a 'normal' zip-based Filevault structure would be
 * something like:
 * <pre>
 * + jcr_root
 *   + archive
 *     + dir
 *       + file.txt
 * + META-INF
 *  </pre>
 * This archive assumes the following, cleaner directory structure instead:
 * <pre>
 * + archive
 *   + dir
 *     + file.txt
 * </pre>
 */
public class WebFilesZipArchive extends AbstractWebFilesArchive {


    private static final Logger log = LoggerFactory.getLogger(WebFilesZipArchive.class);

    private ZipFile zip;
    private final GlobFileNameMatcher includedFileNames;
    private final long maxFileLengthBytes;
    private JarEntry jcrRoot;
    private Entry bundleRoot;

    public WebFilesZipArchive(final ZipFile zip, final GlobFileNameMatcher includedFileNames,  final long maxFileLengthBytes) {
        this.zip = zip;
        this.includedFileNames = includedFileNames;
        this.maxFileLengthBytes = maxFileLengthBytes;
    }

    @Override
    public MetaInf getMetaInf() {
        final DefaultMetaInf metaInf = new DefaultMetaInf();
        metaInf.setSettings(VaultSettings.createDefault());
        return metaInf;
    }

    @Override
    public Entry getRoot() throws IOException {
        return new SingleChildEntry(zip.getName(), jcrRoot);
    }

    @Override
    public Entry getJcrRoot() {
        return jcrRoot;
    }

    @Override
    public Entry getBundleRoot() {
        return bundleRoot;
    }

    public void open(boolean strict) throws IOException {
        jcrRoot = new JarEntry(Constants.ROOT_DIR, true);
        final Enumeration e = zip.entries();
        while (e.hasMoreElements()) {
            final ZipEntry entry = (ZipEntry) e.nextElement();
            final boolean isDirectory = entry.isDirectory();
            final String path = entry.getName();
            final String[] names = Text.explode(path, '/');
            if (names.length > 0 && isIncluded(names, isDirectory, entry.getSize())) {
                JarEntry je = jcrRoot;
                for (int i = 0; i < names.length; i++) {
                    if (i == names.length - 1) {
                        je = je.add(names[i], isDirectory);
                    } else {
                        je = je.add(names[i], true);
                    }
                }
                je.zipEntryName = entry.getName();
                log.debug("scanning jar: {}", je.zipEntryName);
            }
        }
        final Iterator<? extends Entry> jcrRootIterator = jcrRoot.getChildren().iterator();
        if (jcrRootIterator.hasNext()) {
            bundleRoot = jcrRootIterator.next();
        }
    }

    private boolean isIncluded(final String[] names, final boolean lastIsDirectory, final long size) {
        for (int i = 0; i < names.length; i++) {
            final File file = new File(names[i]);
            final boolean isLast = i == names.length - 1;
            final boolean isDirectory = isLast ? lastIsDirectory : true;
            if (!includedFileNames.matches(file.toPath(), isDirectory)) {
                return false;
            }
            if (!isDirectory && size > maxFileLengthBytes) {
                logSizeExceededWarning(file, maxFileLengthBytes);
                return false;
            }
        }
        return true;
    }

    public InputStream openInputStream(Entry entry) throws IOException {
        JarEntry e = (JarEntry) entry;
        if (e == null || e.zipEntryName == null) {
            return null;
        }
        ZipEntry ze = zip.getEntry(e.zipEntryName);
        if (ze == null) {
            throw new IOException("ZipEntry could not be found: " + e.zipEntryName);
        }
        return zip.getInputStream(ze);
    }

    public VaultInputSource getInputSource(Entry entry) throws IOException {
        final JarEntry jarEntry = (JarEntry) entry;
        if (jarEntry == null) {
            throw new IOException("Internal error: jar entry is null");
        } else if (jarEntry.zipEntryName == null) {
            throw new IOException("Internal error: zip entry name of jar entry " + jarEntry.getName() + " is null");
        }
        final ZipEntry zipEntry = zip.getEntry(jarEntry.zipEntryName);
        if (zipEntry == null) {
            throw new IOException("ZipEntry could not be found: " + jarEntry.zipEntryName);
        }
        return new ZipEntryInputSource(zipEntry);
    }

    public void close() {
        try {
            if (zip != null) {
                zip.close();
                zip = null;
            }
        } catch (IOException e) {
            log.warn("Error during close.", e);
        }
    }

    private static class JarEntry implements Entry {

        private final String name;

        private String zipEntryName;

        private final boolean isDirectory;

        private Map<String, JarEntry> children;

        public JarEntry(String name, boolean directory) {
            this.name = defeatNamespaceMangling(name);
            isDirectory = directory;
        }

        public JarEntry add(String name, boolean isDirectory) {
            if (children != null) {
                JarEntry ret = children.get(name);
                if (ret != null) {
                    return ret;
                }
            }
            return add(new JarEntry(name, isDirectory));
        }

        public String getName() {
            return name;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public JarEntry add(JarEntry e) {
            if (children == null) {
                children = new TreeMap<>(BASE_NAME_COMPARATOR);
            }
            children.put(e.getName(), e);
            return e;
        }

        public Collection<? extends Entry> getChildren() {
            return children == null
                    ? Collections.<JarEntry>emptyList()
                    : children.values();
        }

        public Entry getChild(String name) {
            return children == null ? null : children.get(name);
        }

        public String toString() {
            return getName();
        }

    }

    private class ZipEntryInputSource extends VaultInputSource {

        private final ZipEntry zipEntry;

        private ZipEntryInputSource(final ZipEntry zipEntry) {
            super(zipEntry.getName());
            this.zipEntry = zipEntry;
        }

        @Override
        public InputStream getByteStream() {
            try {
                return zip.getInputStream(zipEntry);
            } catch (IOException e1) {
                return null;
            }
        }

        @Override
        public long getContentLength() {
            return zipEntry.getSize();
        }

        @Override
        public long getLastModified() {
            return zipEntry.getTime();
        }

    }
}