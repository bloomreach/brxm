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
package org.onehippo.cm.migration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceProcessor {

    private static final Logger log = LoggerFactory.getLogger(ResourceProcessor.class);

    public void moveGitResource(Path sourcePath, Path destinationPath) throws IOException {

        final Path destinationDir = destinationPath.getParent();
        final File baseDir = sourcePath.getParent().toFile();

        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }
        final Process process = new ProcessBuilder("git", "mv", "-v", "-f", sourcePath.toString(), destinationPath.toString()).directory(baseDir).start();

        final StringBuilder stringBuilder = new StringBuilder();
        try(
                final InputStream is = process.getInputStream();
                final InputStreamReader isr = new InputStreamReader(is);
                final BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }

        String errorMsg = String.format("Error while executing 'git mv -v -f %s %s' command: %s", sourcePath, destinationPath, stringBuilder);
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(errorMsg);
        }

        int exitValue = process.exitValue();
        if (exitValue > 0) {
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    public static void deleteEmptyDirectory(final Path srcDirPath) throws IOException {
        File[] files = srcDirPath.toFile().listFiles();
        if (files != null && files.length == 0) { //delete old directories
            Files.deleteIfExists(srcDirPath);
        }
    }

}
