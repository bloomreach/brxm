/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

/**
 * Configuration parameters for the {@link WebFilesWatcher}.
 */
public interface WebFilesWatcherConfig {

    String[] DEFAULT_INCLUDED_FILES = {};
    String[] DEFAULT_EXCLUDED_DIRECTORIES = {};
    String[] DEFAULT_USE_WATCH_SERVICE_ON_OS_NAMES = {};
    String[] DEFAULT_WATCHED_MODULES = {};
    Long DEFAULT_WATCH_DELAY_MILLIS = 500L;
    Long DEFAULT_MAX_FILE_LENGTH_KB = 256L;

    /**
     * @return the names of the Maven modules that should be watched for changes below the directory
     * <code>src/main/resources</code>. May contain slashes to indicate child modules (e.g "repository-data/webfiles").
     */
    List<String> getWatchedModules();

    /**
     * @return all file name globbing patterns for files in web file bundles that should be watched
     * for changes. The pattern syntax is the same as used in {@link java.nio.file.FileSystem#getPathMatcher}, except
     * that slashes are not allowed (the pattern is supposed to match the file name part of the path only).
     * @see java.nio.file.FileSystem#getPathMatcher
     */
    List<String> getIncludedFiles();

    /**
     * @return all file name globbing patterns for directories in web file bundles that should not be watched
     * for changes. The pattern syntax is the same as used in {@link java.nio.file.FileSystem#getPathMatcher}, except
     * that slashes are not allowed (the pattern is supposed to match the file name part of the path only).
     * All files and sub-directories in excluded directories are excluded too.
     * @see java.nio.file.FileSystem#getPathMatcher
     */
    List<String> getExcludedDirectories();

    /**
     * @return the list of operating system name globbing patterns for which web files should be observed with a
     * {@link java.nio.file.WatchService}. Only use OS names for which a native implementation of the watch service is
     * available without known issues.
     */
    List<String> getUseWatchServiceOnOsNames();

    /**
     * @return the delay in milliseconds between consecutive scans of web files. This parameter is only effective
     * when no {@link java.nio.file.WatchService} is used to watch web files for changes. A lower value will pickup
     * changes faster at the expensive of higher CPU load.
     */
    long getWatchDelayMillis();

    /**
     * @return the maximum allowed file size too use in web files.
     */
    long getMaxFileLengthBytes();
}
