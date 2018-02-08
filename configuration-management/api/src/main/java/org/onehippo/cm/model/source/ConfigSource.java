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
package org.onehippo.cm.model.source;

import java.util.List;

import org.onehippo.cm.model.definition.ConfigDefinition;
import org.onehippo.cm.model.definition.NamespaceDefinition;
import org.onehippo.cm.model.definition.WebFileBundleDefinition;

/**
 * Represents a single configuration source file, which may contain various types of definitions.
 * {@link #getType()} will always return {@link SourceType#CONFIG}.
 */
public interface ConfigSource extends Source {

    /**
     * @return all NamespaceDefinitions in this Source, in serialized format order, in an unmodifiable list
     */
    List<? extends NamespaceDefinition> getNamespaceDefinitions();

    /**
     * @return all WebFileBundleDefinitions in this Source, in serialized format order, in an unmodifiable list
     */
    List<? extends WebFileBundleDefinition> getWebFileBundleDefinitions();

    /**
     * @return all ConfigDefinitions in this Source, in serialized format order, in an unmodifiable list
     */
    List<? extends ConfigDefinition> getConfigDefinitions();

}
