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

package org.onehippo.cms7.essentials.plugin.sdk.service;

import org.onehippo.cms7.essentials.plugin.sdk.model.MavenDependency;
import org.onehippo.cms7.essentials.plugin.sdk.model.TargetPom;

/**
 * Service for checking if a Maven dependency (in the dependencies section) is present, and for adding them otherwise.
 *
 * Can be @Inject'ed into REST Resources and Instructions.
 */
public interface MavenDependencyService {
    /**
     * Check if the specified module already has the specified dependency.
     *
     * @param module     target module to check
     * @param dependency Maven dependency to check for
     * @return           true if dependency already present (and version not older), false otherwise
     */
    boolean hasDependency(TargetPom module, MavenDependency dependency);

    /**
     * If not already present, add the specified Maven dependency to the dependencies section of the specified module.
     *
     * @param module     target module to adjust
     * @param dependency Maven dependency to add
     * @return           true if the specified dependency if present upon returning, false otherwise
     */
    boolean addDependency(TargetPom module, MavenDependency dependency);
}
