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
package org.onehippo.cms7.services.webfiles.watch;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File filter that includes files and excludes directories whose name matches certain globbing patterns.
 * The supported pattern language is the one used by {@link FileSystem#getPathMatcher(String)},
 * except that the path separator '/' should not be used.
 */
public class GlobFileNameMatcher implements FileFilter {

    private static final String GLOB_SYNTAX = "glob:";
    static Logger log = LoggerFactory.getLogger(GlobFileNameMatcher.class);

    private final List<PathMatcher> includedFiles;
    private final List<PathMatcher> excludedDirs;

    public GlobFileNameMatcher() {
        includedFiles = new ArrayList<>();
        excludedDirs = new ArrayList<>();
    }

    public void includeFiles(final List<String> fileNameGlobPatterns) throws IllegalArgumentException {
        for (String pattern : fileNameGlobPatterns) {
            try {
                includeFiles(pattern);
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring file name glob pattern '{}': {}", pattern, e.getMessage());
            }
        }
    }

    public void includeFiles(final String fileNameGlobPattern) throws IllegalArgumentException {
        addPattern(fileNameGlobPattern, includedFiles);
    }

    private static void addPattern(final String fileNameGlobPattern, final List<PathMatcher> matchers) {
        if (fileNameGlobPattern.contains("/")) {
            throw new IllegalArgumentException("cannot contain '/'");
        }
        final FileSystem fs = FileSystems.getDefault();
        final PathMatcher matcher = fs.getPathMatcher(GLOB_SYNTAX + fileNameGlobPattern);
        matchers.add(matcher);
    }

    public void excludeDirectories(final List<String> fileNameGlobPattern) throws IllegalArgumentException {
        for (String pattern : fileNameGlobPattern) {
            try {
                excludeDirectories(pattern);
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring file name glob pattern '{}': {}", pattern, e.getMessage());
            }
        }
    }

    public void excludeDirectories(final String fileNameGlobPattern) throws IllegalArgumentException {
        addPattern(fileNameGlobPattern, excludedDirs);
    }

    public boolean matchesFile(final Path path) {
        return path != null && matches(path.getFileName(), includedFiles);
    }

    public boolean matchesDirectory(final Path path) {
        return path != null && !matches(path.getFileName(), excludedDirs);
    }

    private boolean matches(final Path path, final List<PathMatcher> matchers) {
        if (path == null) {
            return false;
        }
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(final Path path, final boolean isDirectory) {
        return isDirectory ? matchesDirectory(path) : matchesFile(path);
    }

    @Override
    public boolean accept(final File file) {
        return matches(file.toPath(), file.isDirectory());
    }

    public void reset() {
        includedFiles.clear();
        excludedDirs.clear();
    }

}
