/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.components.util;

/**
 * Enum specifying the possible different output modes for the site map generator
 */
public enum OutputMode {

    SPLIT_TO_REPOSITORY(true, false),
    SPLIT_TO_FILE_SYSTEM(true, false),
    SPLIT_TO_TAR_GZ_STREAM(true, true),
    STREAM_SITE_MAP(false, true);

    private final boolean shouldSplit;
    private final boolean outputsToResponse;

    private OutputMode(final boolean shouldSplit, final boolean outputsToResponse) {
        this.shouldSplit = shouldSplit;
        this.outputsToResponse = outputsToResponse;
    }

    public static boolean existsForString(String name) {
        for (OutputMode mode : values()) {
            if (mode.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldSplit() {
        return shouldSplit;
    }

    public boolean outputsToResponse() {
        return outputsToResponse;
    }
}
