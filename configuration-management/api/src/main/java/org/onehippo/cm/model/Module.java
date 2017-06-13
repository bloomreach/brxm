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
package org.onehippo.cm.model;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.onehippo.cm.ResourceInputProvider;

public interface Module extends OrderableByName {

    /**
     * @return the full group/project/module name for this module
     */
    String getFullName();

    /**
     * Modules are composed into Projects and Groups for purposes of expressing dependencies.
     * @return the Project of which this Module is a part
     */
    Project getProject();

    Set<? extends Source> getSources();

    /**
     * @return The immutable set of content {@link Source}s of this module, ordered by path ({@link Source#getPath()})
     *         relative to the module's base resource path.
     */
    Set<? extends Source> getContentSources();

    /**
     * @return The immutable set of config {@link Source}s of this module, ordered by path ({@link Source#getPath()})
     *         relative to the module's base resource path.
     */
    Set<? extends Source> getConfigSources();

    Double getSequenceNumber();

    /**
     * @return A helper object to access raw streams for configuration files.
     */
    ResourceInputProvider getConfigResourceInputProvider();

    /**
     * @return A helper object to access raw streams for content files.
     */
    ResourceInputProvider getContentResourceInputProvider();

    /**
     * Compile a dummy YAML descriptor file to stand in for special case where demo project uses an aggregated
     * descriptor for a set of modules.
     * @return a YAML string representing the group->project->module hierarchy and known dependencies for this Module
     * @throws IOException
     */
    String compileDummyDescriptor();

    /**
     * @return The immutable map of action items per version
     */
    Map<Double, Set<ActionItem>> getActionsMap();

    Set<String> getRemovedConfigResources();

    Set<String> getRemovedContentResources();

}
