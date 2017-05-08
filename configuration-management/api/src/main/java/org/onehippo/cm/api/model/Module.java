/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.api.model;

import java.io.IOException;
import java.util.Set;

public interface Module extends Orderable {

    Project getProject();

    Set<Source> getSources();

    /**
     * @return The immutable set of content {@link Source}s of this module, ordered by path ({@link Source#getPath()})
     *         relative to the module's base resource path.
     */
    Set<Source> getContentSources();

    /**
     * @return The immutable set of config {@link Source}s of this module, ordered by path ({@link Source#getPath()})
     *         relative to the module's base resource path.
     */
    Set<Source> getConfigSources();


    /**
     * Compile a dummy YAML descriptor file to stand in for special case where demo project uses an aggregated
     * descriptor for a set of modules.
     * @return a YAML string representing the group->project->module hierarchy and known dependencies for this Module
     * @throws IOException
     */
    String compileDummyDescriptor();

}
