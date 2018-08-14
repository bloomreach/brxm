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
package org.onehippo.cm.engine;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Zip utility
 */
public class ZipCompressor {

    /**
     * Create {@link com.sun.nio.zipfs.ZipFileSystem} from file
     * @param zipFilename absolute path to zip file
     * @param create create new or reuse existing
     * @return {@link com.sun.nio.zipfs.ZipFileSystem}
     */
    public static FileSystem createZipFileSystem(final String zipFilename, final boolean create) throws IOException {

        final Path path = Paths.get(zipFilename);
        // todo: is this the right thing to do on Windows?
        final URI uri = URI.create("jar:file:" + path.toUri().getPath());

        final Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
        }
        return FileSystems.newFileSystem(uri, env);
    }

    /**
     * Zip directory's contents
     *
     * @param dir directory to compress
     * @param targetZip target path of the zip file
     */
    public void zipDirectory(final Path dir, final String targetZip) throws IOException {
        final List<String> paths = getFilesInDirectory(dir);
        try (final FileOutputStream fos = new FileOutputStream(targetZip);
             final ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (final String filePath : paths) {
                // zip file paths use unix folder seperators regardless from the operating system
                final ZipEntry ze = new ZipEntry(filePath
                        .substring(dir.toAbsolutePath().toString().length() + 1, filePath.length()).replace("\\", "/"));
                zos.putNextEntry(ze);

                try (FileInputStream fis = new FileInputStream(filePath)) {
                    IOUtils.copy(fis, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    /**
     * Collect all the files in a directory and its subdirectories
     *
     * @param dir directory
     */
    private List<String> getFilesInDirectory(final Path dir) throws IOException {
        final List<String> paths = new ArrayList<>();
        final BiPredicate<Path, BasicFileAttributes> matcher =
                (filePath, fileAttr) -> fileAttr.isRegularFile();
        Files.find(dir, Integer.MAX_VALUE, matcher).forEachOrdered(p -> paths.add(p.toString()));
        return paths;
    }
}

