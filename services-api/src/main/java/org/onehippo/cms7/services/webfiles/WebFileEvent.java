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
package org.onehippo.cms7.services.webfiles;

import java.nio.file.Path;

@SuppressWarnings("ALL")
public class WebFileEvent {

    private final Path changedPath;
    private final Path watchedRootDir;

    public WebFileEvent(final Path changedPath, final Path watchedRootDir) {
        this.changedPath = changedPath;
        this.watchedRootDir = watchedRootDir;
    }

    public Path getWatchRootDir() {
        return watchedRootDir;
    }

    public Path getChangedPath() {
        return changedPath;
    }

    public Path getRelativeChangedPath() {
        return watchedRootDir.relativize(changedPath);
    }
}
