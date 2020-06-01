/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.service;

import org.onehippo.cms7.essentials.sdk.api.model.Module;

/**
 * Service for adjusting basic parameters of a Maven pom.xml file.
 *
 * Can be @Inject'ed into REST Resources and Instructions.
 */
public interface MavenModelService {

    /**
     * Set the parent project parameters of the specified module.
     *
     * @param module     project module to adjust
     * @param groupId    parent project group ID
     * @param artifactId parent project artifact ID
     * @param version    (optional) parent project version
     * @return true if the module's parent project parameters match the specified parameters upon returning, false otherwise.
     */
    boolean setParentProject(Module module, String groupId, String artifactId, String version);
}
