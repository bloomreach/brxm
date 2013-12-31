/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

public class EnhancedSystemViewPackage {

    private final File archive;
    private final File xml;
    private final Map<String, File> binaries;

    private EnhancedSystemViewPackage(final File archive, final File xml, final Map<String, File> binaries) {
        this.archive = archive;
        this.xml = xml;
        this.binaries = binaries;
    }

    public static EnhancedSystemViewPackage create(File archive) throws IOException {
        File xml = null;
        final Map<String, File> binaries = new HashMap<>();
        final ZipFile zipFile = new ZipFile(archive);
        try {
            final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            if (!zipEntries.hasMoreElements()) {
                throw new IOException("Archive is empty");
            }

            while (zipEntries.hasMoreElements()) {
                final ZipEntry zipEntry = zipEntries.nextElement();
                if (zipEntry.getName().endsWith(".xml")) {
                    if (xml != null) {
                        throw new IOException("Expected only one xml file in the archive");
                    }
                    final File file = File.createTempFile("scope", ".xml");
                    final InputStream in = zipFile.getInputStream(zipEntry);
                    final FileOutputStream out = new FileOutputStream(file);
                    try {
                        IOUtils.copy(in, out);
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);
                    }
                    xml = file;
                } else {
                    final File file = File.createTempFile("binary", ".bin");
                    final InputStream in = zipFile.getInputStream(zipEntry);
                    final FileOutputStream out = new FileOutputStream(file);
                    try {
                        IOUtils.copy(in, out);
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);
                    }
                    binaries.put(zipEntry.getName(), file);
                }
            }
        } finally {
            try { zipFile.close(); } catch (IOException ignore) {}
        }

        if (xml == null) {
            throw new IOException("Did not find an xml file in the archive");
        }

        return new EnhancedSystemViewPackage(archive, xml, binaries);
    }

    public static EnhancedSystemViewPackage create(File xml, Collection<File> binaries) throws IOException {
        final File archive = File.createTempFile("esv", ".zip");
        final ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(archive));

        zipOut.putNextEntry(new ZipEntry("esv.xml"));
        final FileInputStream xmlIn = new FileInputStream(xml);
        try {
            IOUtils.copy(xmlIn, zipOut);
        } finally {
            IOUtils.closeQuietly(xmlIn);
        }
        for (File binary : binaries) {
            zipOut.putNextEntry(new ZipEntry(binary.getName()));
            final FileInputStream binIn = new FileInputStream(binary);
            try {
                IOUtils.copy(binIn, zipOut);
            } finally {
                IOUtils.closeQuietly(binIn);
            }
        }
        IOUtils.closeQuietly(zipOut);

        return new EnhancedSystemViewPackage(archive, xml, collectionToMap(binaries));
    }

    private static Map<String, File> collectionToMap(Collection<File> collection) {
        final Map<String, File> map = new HashMap<>();
        for (final File file : collection) {
            map.put(file.getName(), file);
        }
        return map;
    }

    public File getArchive() {
        return archive;
    }

    public File getXml() {
        return xml;
    }

    public Map<String, File> getBinaries() {
        return binaries;
    }

}
