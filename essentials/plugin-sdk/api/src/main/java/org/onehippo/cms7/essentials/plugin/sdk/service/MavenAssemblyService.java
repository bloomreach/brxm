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

/**
 * MavenAssemblyService provides methods to manipulate Maven Assembly descriptor files.
 *
 * It can be @Inject-ed into an Essentials plugin's REST resource or custom {@code Instruction}.
 */
public interface MavenAssemblyService {
    /**
     * Add a &lt;dependencySet&gt; element to the specified descriptor file.
     *
     * This method assumes that a &lt;dependencySets&gt; element is found (or created) at the root of the descriptor.
     *
     * @param descriptorFilename    name of the descriptor file in the project's directory for assembly descriptors
     * @param outputDirectory       value for the &lt;outputDirectory&gt; element
     * @param outputFileNameMapping value for the &lt;outputFileNameMapping&gt; element
     * @param useProjectArtifact    boolean value for the &lt;useProjectArtifact&gt; element
     * @param scope                 value for the &lt;scope&gt; element
     * @param dependency            dependency to wrap in a &lt;includes&gt;&lt;include&gt; element
     * @return                      true if &lt;dependencySet&gt; was added successfully, false otherwise
     */
    boolean addDependencySet(String descriptorFilename, String outputDirectory, String outputFileNameMapping,
                             boolean useProjectArtifact, String scope, MavenDependency dependency);

    /**
     * Add a &lt;include&gt; element to the first &lt;dependencySet&gt; in the specified descriptor file.
     *
     * Note that dependency sets do not seem to have a unique identifier, so the best we can do is to manipulate
     * a predefined one - the first one in this case. We only add the &lt;include&gt; if no &lt;include&gt; with
     * the same value exists.
     *
     * @param descriptorFilename name of the descriptor file in the project's directory for assembly descriptors
     * @param dependency         dependency to be included
     * @return                   true if &lt;include&gt; was added successfully, false otherwise
     */
    boolean addIncludeToFirstDependencySet(String descriptorFilename, MavenDependency dependency);
}
