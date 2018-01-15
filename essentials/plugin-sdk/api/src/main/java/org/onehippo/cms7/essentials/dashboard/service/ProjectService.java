/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.service;

import java.io.File;
import java.util.List;

/**
 * ProjectService provides access to the project, and in particular to the file resources of a project.
 *
 * It can be @Inject-ed into an Essentials plugin's REST resource or custom {@code Instruction}.
 */
public interface ProjectService {
    final String GROUP_ID_COMMUNITY = "org.onehippo.cms7";
    final String GROUP_ID_ENTERPRISE = "com.onehippo.cms7";

    /**
     * Retrieve a list of the log4j2 files of the project.
     */
    List<File> getLog4j2Files();
}
