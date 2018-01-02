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
package org.onehippo.cm.model.source;

import java.util.List;

import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.definition.Definition;

/**
 * Represents a single source file as stored within a Module and its parsed content.
 */
public interface Source {

    /**
     * @return the type of this source (config or content)
     */
    SourceType getType();

    /**
     * @return the relative path of this source to the {@link Module} base path, not starting with '/'
     */
    String getPath();

    /**
     * @return the folder path relative to the module base containing this source, not starting with '/', might be empty
     *      (will be empty if {@link #getPath()} does not contain '/' indicating source is in the root folder)
     */
    String getFolderPath();

    /**
     * @return the module that defines this source
     */
    Module getModule();

    /**
     * @return The <strong>ordered</strong> List of {@link Definition}s for this {@link Source} as an immutable list
     * or empty immutable list if none present. Note the ordering is according to serialized yaml format and not in
     * model processing order.
     */
    List<? extends Definition> getDefinitions();

    /**
     * @return a String describing this item for error-reporting purposes
     */
    String getOrigin();

}
