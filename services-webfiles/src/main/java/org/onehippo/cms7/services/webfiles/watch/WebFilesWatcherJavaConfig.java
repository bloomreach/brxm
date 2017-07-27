/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.onehippo.cms7.services.webfiles.WebFilesService;

public class WebFilesWatcherJavaConfig implements WebFilesWatcherConfig {

    private final List<String> watchedModules;
    private final List<String> includedFiles;
    private final List<String> excludedDirs;
    private final List<String> useWatchServiceOnOsNames;
    private long watchDelayMillis;
    private long maxFileLengthBytes;

    public WebFilesWatcherJavaConfig() {
        watchedModules = new ArrayList<>(Arrays.asList(DEFAULT_WATCHED_MODULES));
        includedFiles = new ArrayList<>(Arrays.asList(DEFAULT_INCLUDED_FILES));
        excludedDirs = new ArrayList<>(Arrays.asList(DEFAULT_EXCLUDED_DIRECTORIES));
        useWatchServiceOnOsNames = new ArrayList<>(Arrays.asList(DEFAULT_USE_WATCH_SERVICE_ON_OS_NAMES));
        watchDelayMillis = DEFAULT_WATCH_DELAY_MILLIS;
        maxFileLengthBytes = 1024 * DEFAULT_MAX_FILE_LENGTH_KB;
    }

    void addWatchedModule(final String module) {
        watchedModules.add(module);
    }

    public void includeFiles(final String... globPatterns) {
        Collections.addAll(includedFiles, globPatterns);
    }

    public void excludeDirs(final String... globPatterns) {
        Collections.addAll(excludedDirs, globPatterns);
    }

    void useWatchServiceOnOsNames(final String... osNames) {
        Collections.addAll(useWatchServiceOnOsNames, osNames);
    }

    void setWatchDelayMillis(final long delayMillis) {
        watchDelayMillis = delayMillis;
    }

    @Override
    public List<String> getWatchedModules() {
        return watchedModules;
    }

    @Override
    public String getReloadMode() {
        return WebFilesService.RELOAD_NEVER;
    }

    @Override
    public List<String> getIncludedFiles() {
        return includedFiles;
    }

    @Override
    public List<String> getExcludedDirectories() {
        return excludedDirs;
    }

    @Override
    public List<String> getUseWatchServiceOnOsNames() {
        return useWatchServiceOnOsNames;
    }

    @Override
    public long getWatchDelayMillis() {
        return watchDelayMillis;
    }

    @Override
    public long getMaxFileLengthBytes() {
        return maxFileLengthBytes;
    }
}
