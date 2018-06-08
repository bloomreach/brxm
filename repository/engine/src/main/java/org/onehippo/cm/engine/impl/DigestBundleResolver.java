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
package org.onehippo.cm.engine.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.util.Text;
import org.onehippo.cm.model.util.DigestUtils;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.onehippo.repository.util.PartialZipFile;
import org.onehippo.repository.util.JcrConstants;

import static org.onehippo.repository.util.JcrConstants.JCR_CONTENT;
import static org.onehippo.repository.util.JcrConstants.JCR_DATA;

/**
 * Helper methods for dealing with bundle digest calculation/retrieval
 */
public class DigestBundleResolver {

    public static String calculateFsBundleDigest(final PartialZipFile bundleZipFile, final WebFilesService webfilesService) throws IOException {
        final Map<String, String> jarBundle = new TreeMap<>();
        final Enumeration<? extends ZipEntry> entries = bundleZipFile.entries();

        while (entries.hasMoreElements()) {
            final ZipEntry zipEntry = entries.nextElement();
            final boolean isDirectory = zipEntry.isDirectory();
            final String path = zipEntry.getName();
            final String[] names = Text.explode(path, '/');
            if (names.length > 0 && isIncluded(names, isDirectory, zipEntry.getSize(), webfilesService)) {
                if (!isDirectory) {
                    try (final InputStream inputStream = bundleZipFile.getInputStream(zipEntry)) {
                        final String digest = DigestUtils.digestFromStream(inputStream);
                        jarBundle.put(zipEntry.getName(), digest);
                    }
                }
            }
        }

        final String jarChecksum = jarBundle.keySet().stream().map(jarBundle::get).collect(Collectors.joining());
        return DigestUtils.computeManifestDigest(jarChecksum);
    }

    private static boolean isIncluded(final String[] names, final boolean lastIsDirectory, final long size, final WebFilesService webfilesService) {

        class BundleFile extends File {

            private final long size;
            private final boolean isDirectory;

            private BundleFile(final String pathname, boolean isDirectory, long size) {
                super(pathname);
                this.isDirectory = isDirectory;
                this.size = size;
            }

            @Override
            public boolean isDirectory() {
                return isDirectory;
            }

            @Override
            public boolean isFile() {
                return !isDirectory;
            }

            @Override
            public long length() {
                return size;
            }
        }

        for (int i = 0; i < names.length; i++) {
            final boolean isLast = i == names.length - 1;
            final boolean isDirectory = !isLast || lastIsDirectory;
            final File file = new BundleFile(names[i], isDirectory, size);
            if (!webfilesService.fileMatches(file)) {
                return false;
            }
        }
        return true;
    }

    public static String calculateRuntimeBundleDigest(final Node bundleNode) throws IOException, RepositoryException {
        final Map<String, String> fileDigestMap = new TreeMap<>();
        collectBundleDigests(bundleNode, fileDigestMap);
        final String collectiveChecksum = fileDigestMap.keySet().stream().map(fileDigestMap::get).collect(Collectors.joining());
        return DigestUtils.computeManifestDigest(collectiveChecksum);
    }

    private static void collectBundleDigests(final Node bundleNode, final Map<String, String> sortedMap) throws RepositoryException, IOException {
        for (NodeIterator pni = bundleNode.getNodes(); pni.hasNext(); ) {
            Node nextNode = pni.nextNode();
            if (nextNode.getPrimaryNodeType().isNodeType(JcrConstants.NT_FOLDER)) {
                collectBundleDigests(nextNode, sortedMap);
            } else if (nextNode.getPrimaryNodeType().isNodeType(JcrConstants.NT_FILE)) {
                try (final InputStream stream = nextNode.getNode(JCR_CONTENT).getProperty(JCR_DATA).getValue().getBinary().getStream()) {
                    final String digest = DigestUtils.digestFromStream(stream);
                    sortedMap.put(nextNode.getPath(), digest);
                }
            }
        }
    }
}
