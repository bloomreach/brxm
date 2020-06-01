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
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.config.DefaultMetaInf;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.config.VaultSettings;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.util.FileInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.services.webfiles.vault.FileNameComparatorUtils.FILE_BASE_NAME_COMPARATOR;

/**
 * Filevault archive based on a directory on the local filesystem. The directory itself is the only root directory
 * to import, and there is no meta-data directory read. For example, a 'normal' file-based Filevault directory
 * structure would be something like:
 * <pre>
 * + archive
 *   + jcr_root
 *     + archive
 *       + dir
 *         + file.txt
 *   + META-INF
 *  </pre>
 *  This archive assumes the following, cleaner directory structure instead:
 *  <pre>
 *  + archive
 *    + dir
 *      + file.txt
 *  </pre>
 *  The name 'archive' is used for the both the name of the root entry, and the single child of the 'virtual'
 *  jcr_root directory.
 */
public class WebFilesFileArchive extends AbstractWebFilesArchive {

    private final File directory;
    private final FileFilter includedFiles;
    private final long maxFileLengthBytes;
    private Entry root;
    private Entry jcrRoot;
    private FileEntry bundleRoot;

    public WebFilesFileArchive(final File directory, final FileFilter includedFiles, final long maxFileLengthBytes) {
        this.directory = directory;
        this.includedFiles = includedFiles;
        this.maxFileLengthBytes = maxFileLengthBytes;
    }

    @Override
    public MetaInf getMetaInf() {
        final DefaultMetaInf metaInf = new DefaultMetaInf();
        metaInf.setSettings(VaultSettings.createDefault());
        return metaInf;
    }

    @Override
    public Entry getJcrRoot() {
        return jcrRoot;
    }

    @Override
    public Entry getRoot() {
        return root;
    }

    @Override
    public Entry getBundleRoot() {
        return bundleRoot;
    }

    public void open(boolean strict) {
        bundleRoot = new FileEntry(directory, includedFiles, maxFileLengthBytes);
        jcrRoot = new SingleChildEntry(Constants.ROOT_DIR, bundleRoot);
        root = new SingleChildEntry(directory.getName(), jcrRoot);
    }

    public void close() {
        // do nothing
    }

    public InputStream openInputStream(Entry entry) throws IOException {
        if (entry == null || entry == root) {
            return null;
        } else if (entry == jcrRoot) {
            return FileUtils.openInputStream(directory);
        } else {
            File file = ((FileEntry) entry).file;
            if (file == null || !includedFiles.accept(file) || !file.isFile() || !file.canRead() || !file.exists()) {
                throw new IOException(String.format("Can't read file '%s'", file.getPath()));
            }
            return FileUtils.openInputStream(file);
        }
    }

    public VaultInputSource getInputSource(Entry entry) throws IOException {
        if (entry == null) {
            throw new IOException("Cannot create input source for null entry");
        } else if (entry == root) {
            throw new IOException("Cannot create input source for root entry");
        } else if (entry == jcrRoot) {
            return new FileInputSource(directory);
        } else {
            File file = ((FileEntry) entry).file;
            if (file == null) {
                throw new IOException("Internal error: entry's file is null");
            } else if (!file.exists()) {
                throw new IOException("File " + file.getPath() + " no longer exists");
            } else if (!file.isFile()) {
                throw new IOException(file.getPath() + " is not a file");
            } else if (!file.canRead()) {
                throw new IOException("Cannot read file " + file.getPath());
            }
            return new FileInputSource(file);
        }
    }

    private static class FileEntry implements Entry {

        private final File file;
        private final FileFilter includedFiles;
        private final long maxFileLengthBytes;

        private FileEntry(final File file, final FileFilter includedFiles, final long maxFileLengthBytes) {
            this.file = file;
            this.includedFiles = includedFiles;
            this.maxFileLengthBytes = maxFileLengthBytes;
        }

        public String getName() {
            return defeatNamespaceMangling(file.getName());
        }

        public boolean isDirectory() {
            return file.isDirectory();
        }

        public Collection<Entry> getChildren() {
            final File[] files = file.listFiles(includedFiles);
            if (files == null || files.length == 0) {
                return null;
            }
            Arrays.sort(files, FILE_BASE_NAME_COMPARATOR);
            final List<Entry> children = new ArrayList<>(files.length);
            for (File file: files) {
                if (file.isFile() && file.length() > maxFileLengthBytes) {
                    logSizeExceededWarning(file, maxFileLengthBytes);
                    continue;
                }
                children.add(new FileEntry(file, includedFiles, maxFileLengthBytes));
            }
            return children;
        }

        public Entry getChild(String name) {
            final File child = new File(file, name);
            if (includedFiles.accept(child) && child.exists()) {
                if (child.isFile() && child.length() > maxFileLengthBytes) {
                    logSizeExceededWarning(file, maxFileLengthBytes);
                    return null;
                }
                return new FileEntry(child, includedFiles, maxFileLengthBytes);
            }
            return null;
        }

        public String toString() {
            return getName();
        }

    }

}
