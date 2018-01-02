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
package org.onehippo.cm.model.definition;

import java.net.URI;

import org.onehippo.cm.model.source.ConfigSource;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.source.SourceType;
import org.onehippo.cm.model.tree.Value;

/**
 * Represents the definition of a JCR node type namespace and (potentially) an associated CND resource defining JCR node
 * types in a {@link Source} of type {@link SourceType#CONFIG}.
 */
public interface NamespaceDefinition<S extends ConfigSource> extends Definition<S> {

    /**
     * @return the JCR name prefix for this namespace
     */
    String getPrefix();

    /**
     * @return the JCR namespace URI for this namespace
     */
    URI getURI();

    /**
     * @return the resource path to a CND file containing JCR node type definitions, or null if this namespace is not
     *     associated with any node type definitions (e.g. if it is only used for property names)
     */
    Value getCndPath();

}
