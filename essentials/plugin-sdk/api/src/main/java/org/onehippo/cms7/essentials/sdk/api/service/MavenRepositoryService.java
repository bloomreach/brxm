/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cms7.essentials.sdk.api.rest.MavenRepository;
import org.onehippo.cms7.essentials.sdk.api.service.model.Module;

/**
 * Service for checking if a Maven repository is present, and for adding them otherwise.
 *
 * Can be @Inject'ed into REST Resources and Instructions.
 */
public interface MavenRepositoryService {
    /**
     * Ensure that the specified repository exists in the target module's pom.xml file.
     *
     * @param module     target module to adjust
     * @param repository repository entry to add, if not already present
     * @return           true if the repository exists in the specified module upon returning, false otherwise
     */
    boolean addRepository(final Module module, final MavenRepository repository);
}
