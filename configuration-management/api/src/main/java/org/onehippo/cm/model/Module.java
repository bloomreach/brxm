/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.onehippo.cm.model.definition.ActionItem;
import org.onehippo.cm.model.source.ConfigSource;
import org.onehippo.cm.model.source.ContentSource;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.Source;

/**
 * Represents the atomic deployable unit in the Hippo Configuration Management (HCM) system. This is intended to equate
 * conceptually to the level of Maven modules and artifact IDs in that dependency management system.
 */
public interface Module extends OrderableByName {

    /**
     * @return the full group/project/module name for this module
     */
    String getFullName();

    /**
     * @return Returns true if this module is loaded from a zip archive (jar), otherwise false
     */
    boolean isArchive();

    /**
     * @return Returns the {@link File} handle to the module zip archive (jar) if {@link #isArchive()}, otherwise null
     */
    File getArchiveFile();

    /**
     * Modules are composed into Projects and Groups for purposes of expressing dependencies.
     * @return the Project of which this Module is a part
     */
    Project getProject();

    /**
     * @return Extension name of the Module or null if it is a core Module
     */
    String getExtension();

    /**
     * @return The immutable set of all {@link Source}s of this module, in undefined order.
     */
    Set<? extends Source> getSources();

    /**
     * @return The immutable set of content {@link Source}s of this module, ordered by path ({@link Source#getPath()})
     *         relative to the module's base resource path.
     */
    Set<? extends ContentSource> getContentSources();

    /**
     * @return The immutable set of config {@link Source}s of this module, ordered by path ({@link Source#getPath()})
     *         relative to the module's base resource path.
     */
    Set<? extends ConfigSource> getConfigSources();


    /**
     * @return A helper object to access raw streams for configuration files.
     */
    ResourceInputProvider getConfigResourceInputProvider();

    /**
     * @return A helper object to access raw streams for content files.
     */
    ResourceInputProvider getContentResourceInputProvider();

    /**
     * @return The immutable map of action items per version, which describe how to handle content source bootstrapping
     */
    Map<String, Set<ActionItem>> getActionsMap();

    /**
     * @return the current "sequence number" of this module, which describes the most recent set of actions from
     * {@link #getActionsMap()} that have been applied to the JCR
     */
    String getLastExecutedAction();

}
