/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;

public class ZipCompressorTest {

    @Test
    public void checkCompressedZipFileDoesNotContainWindowsFolderSeperator() throws Exception {
        Path rootPath = Files.createTempDirectory("zipContentPath");
        Files.createTempFile(rootPath, "yamlFile", ".yaml");
        Path folderPath = Files.createTempDirectory(rootPath, "folderPath");
        Files.createTempFile(folderPath, "textFile", ".txt");

        final ZipCompressor zipCompressor = new ZipCompressor();
        final File file = File.createTempFile("zipFile", ".zip");
        zipCompressor.zipDirectory(rootPath, file.getAbsolutePath());

        ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            assertFalse(zipEntry.getName().contains("\\"));
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }
}
