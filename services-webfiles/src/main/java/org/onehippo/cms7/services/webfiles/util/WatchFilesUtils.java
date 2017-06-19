/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.util;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.onehippo.cms7.services.webfiles.watch.WebFilesWatcherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WatchFilesUtils {

    private static final Logger log = LoggerFactory.getLogger(WatchFilesUtils.class);

    public static final String PROJECT_BASEDIR_PROPERTY = "project.basedir";
    public static final String WEB_FILES_LOCATION_IN_MODULE = "src/main/resources/hcm-config";

    public static Path getProjectBaseDir() {
        final String projectBaseDir = System.getProperty(PROJECT_BASEDIR_PROPERTY);
        if (projectBaseDir != null && !projectBaseDir.isEmpty()) {
            final Path baseDir = FileSystems.getDefault().getPath(projectBaseDir);
            if (Files.isDirectory(baseDir)) {
                return baseDir;
            } else {
                log.warn("Watching web files is disabled: environment variable '{}' does not point to a directory", PROJECT_BASEDIR_PROPERTY);
            }
        } else {
            log.info("Watching web files is disabled: environment variable '{}' not set or empty", PROJECT_BASEDIR_PROPERTY);
        }
        return null;
    }

    public static List<Path> getWebFilesDirectories(final Path projectBaseDir,
                                              final WebFilesWatcherConfig config) {

        List<Path> webFilesDirectories = new ArrayList<>(config.getWatchedModules().size());
        for (String watchedModule : config.getWatchedModules()) {
            final Path webFilesModule = projectBaseDir.resolve(watchedModule);
            final Path webFilesDirectory = webFilesModule.resolve(WEB_FILES_LOCATION_IN_MODULE);
            if (Files.isDirectory(webFilesDirectory)) {
                webFilesDirectories.add(webFilesDirectory);
            } else {
                log.warn("Cannot watch web files in module '{}': it does not contain directory '{}'",
                        watchedModule, WEB_FILES_LOCATION_IN_MODULE);
            }
        }
        return webFilesDirectories;
    }
}
