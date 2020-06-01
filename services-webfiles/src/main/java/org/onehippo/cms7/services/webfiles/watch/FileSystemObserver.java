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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Observes registered directories for changes (created, modified en deleted files and directories)
 * and notifies listeners of the observed changes.
 */
public interface FileSystemObserver {

    /**
     * Registers a directory for observation (recursively).
     * @param directory the directory to register
     * @param listener the listener to call when files or directories below the directory changes
     * @throws IOException when an I/O error occurs while registering the directory for observation
     */
    public void registerDirectory(Path directory, FileSystemListener listener) throws IOException;

    /**
     * @return the root {@link java.nio.file.Path} directories which are registered via
     * {@link #registerDirectory(java.nio.file.Path, FileSystemListener)}
     */
    public List<Path> getObservedRootDirectories();

    /**
     * Closes resources used by this observer.
     */
    public void shutdown();

}
