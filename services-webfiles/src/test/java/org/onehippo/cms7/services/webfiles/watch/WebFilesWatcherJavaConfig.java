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

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cms7.services.webfiles.watch.WebFilesWatcherConfig;

public class WebFilesWatcherJavaConfig implements WebFilesWatcherConfig {

    private final List<String> watchedModules;
    private final List<String> includedFiles;
    private final List<String> excludedDirs;
    private final List<String> useWatchServiceOnOsNames;
    private long watchDelayMillis;

    WebFilesWatcherJavaConfig() {
        watchedModules = new ArrayList<>();
        includedFiles = new ArrayList<>();
        excludedDirs = new ArrayList<>();
        useWatchServiceOnOsNames = new ArrayList<>();
        watchDelayMillis = 0;
    }

    void addWatchedModule(final String module) {
        watchedModules.add(module);
    }

    void includeFiles(final String... globPatterns) {
        for (String pattern : globPatterns) {
            includedFiles.add(pattern);
        }
    }

    void excludeDirs(final String... globPatterns) {
        for (String pattern : globPatterns) {
            excludedDirs.add(pattern);
        }
    }

    void useWatchServiceOnOsNames(final String... osNames) {
        for (String osName : osNames) {
            useWatchServiceOnOsNames.add(osName);
        }
    }

    void setWatchDelayMillis(final long delayMillis) {
        watchDelayMillis = delayMillis;
    }

    @Override
    public List<String> getWatchedModules() {
        return watchedModules;
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
}
