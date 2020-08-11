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

import org.onehippo.cm.model.definition.Definition;
import org.onehippo.cm.model.definition.DefinitionType;

/**
 * Describes whether this is a content source (containing only {@link DefinitionType#CONTENT} definitions) or a config
 * source (containing any of the other {@link DefinitionType}s.
 */
public enum SourceType {

    /**
     * Describes a Source (potentially) containing any {@link DefinitionType} other than {@link DefinitionType#CONTENT}.
     */
    CONFIG,

    /**
     * Describes a Source that may contain exactly one {@link Definition} of {@link DefinitionType#CONTENT}.
     */
    CONTENT;

    /**
     * @param source a Source whose type we want to check
     * @return true iff {@link Source#getType()} == this
     */
    public final boolean isOfType(final Source source) {
        return this == source.getType();
    }

}