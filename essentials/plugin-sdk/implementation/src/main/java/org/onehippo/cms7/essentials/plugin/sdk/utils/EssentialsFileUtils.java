/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EssentialsFileUtils {

    private static final Logger log = LoggerFactory.getLogger(EssentialsFileUtils.class);

    public static void createParentDirectories(final File directory) throws IOException {
        if (directory == null) {
            throw new IllegalArgumentException("Directory was null");
        }
        final Deque<String> directories = new ArrayDeque<>();
        String targetDirectory = directory.getAbsolutePath();
        while (!new File(targetDirectory).exists()) {
            directories.push(targetDirectory);
            targetDirectory = new File(targetDirectory).getParent();
        }
        processDirectories(directories);
    }

    /**
     * Deletes director only if content of the directory doesn't contain any child directories
     * @param directory  directory to delete
     * @return boolean on success
     */
    public static boolean deleteSingleDirectory(final File directory) {
        if (directory == null) {
            throw new IllegalArgumentException("Directory was null");
        }
        if (!directory.exists()) {
            log.warn("directory {} doesn't exist", directory);
            return false;
        }
        if (!directory.isDirectory()) {
            log.warn("file {} is not a directory", directory);
            return false;
        }
        final File[] files = directory.listFiles();
        if (files !=null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    throw new IllegalArgumentException("Directory contains directories");
                }
            }
            // delete files:
            for (File file : files) {
                file.delete();
            }
        }
        return directory.delete();
    }

    private static void processDirectories(final Deque<String> directories) throws IOException {
        if (!directories.isEmpty()) {
            Files.createDirectories(new File(directories.getLast()).toPath());
        }
    }



}
