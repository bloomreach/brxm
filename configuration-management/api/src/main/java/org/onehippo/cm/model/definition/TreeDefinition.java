/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.definition;

import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.tree.DefinitionNode;

/**
 * Represents the definition of a JCR node tree in a {@link Source}, including metadata.
 * {@link #getType()} may return {@link DefinitionType#CONFIG} or {@link DefinitionType#CONTENT}, depending on context.
 */
public interface TreeDefinition<S extends Source> extends Definition<S>, Comparable<TreeDefinition> {

    /**
     * @return the root node of this definition
     */
    DefinitionNode getNode();

    /**
     * The effective root path of this tree definition, which <em>may</em> be different from the
     * {@link DefinitionNode#getPath()} of the {@link #getNode() definition root node}.
     * <p>
     * This can have been set/used for 'relative' content import/export definitions, while also keeping track
     * (in process, transient only) of the absolute root path as might be needed to resolve/map absolute references
     * relatively.
     * </p>
     * @return the effective root path of this definition.
     */
    JcrPath getRootPath();

}
