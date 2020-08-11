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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

/**
 * Matches the name of the current OS with a set of globbing patterns.
 */
public class OsNameMatcher {

    private static final String GLOB_SYNTAX = "glob:";

    private final List<PathMatcher> included;
    private Path currentOsPath;

    public OsNameMatcher() {
        included = new ArrayList<>();
        setCurrentOsName(System.getProperty("os.name"));
    }

    void setCurrentOsName(final String osName) {
        currentOsPath = FileSystems.getDefault().getPath(osName);
    }

    public void include(final String osNameGlobPattern) {
        if (osNameGlobPattern.contains("/")) {
            throw new IllegalArgumentException("OS name pattern cannot contain '/'");
        }

        final FileSystem fs = FileSystems.getDefault();

        final PathMatcher includeOsNames = fs.getPathMatcher(GLOB_SYNTAX + osNameGlobPattern);
        included.add(includeOsNames);
    }

    public boolean matchesCurrentOs() {
        for (PathMatcher includedMatcher : included) {
            if (includedMatcher.matches(currentOsPath)) {
                return true;
            }
        }
        return false;
    }

}
