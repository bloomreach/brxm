/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.services.webfiles.watch;

import java.nio.file.Path;
import java.util.List;

import org.onehippo.cms7.services.SingletonService;
import org.onehippo.cms7.services.WhiteboardService;

/**
 * Provides access to service used in development environments to watch for changes in webfiles sources.
 */
@SingletonService
@WhiteboardService
public interface WebFilesWatcherService {

    /**
     * @return List of webfile module directories
     */
    List<Path> getWebFilesDirectories();
}
