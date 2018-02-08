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
package org.onehippo.cm.model.tree;

/**
 * Represents the operation to be performed when merging a {@link DefinitionProperty} with previously-existing
 * definitions to produce a {@link ConfigurationProperty}.
 */
public enum PropertyOperation {

    /**
     * For a multi-valued property, add values defined in this {@link DefinitionProperty} to existing values.
     * This operation should not be used for single-valued properties.
     */
    ADD,

    /**
     * This {@link DefinitionProperty} represents a deletion of an existing property, and thus no values should
     * be specified.
     */
    DELETE,

    /**
     * This {@link DefinitionProperty} represents a (potential) change in multiplicity and a complete replacement of
     * values defined previously for this property.
     */
    OVERRIDE,

    /**
     * Replace all previously-defined values for this property with the values defined on this {@link DefinitionProperty}.
     * This is the default operation where none is explicitly specified.
     */
    REPLACE;

    public final String toString() {
        return name().toLowerCase();
    }

}
