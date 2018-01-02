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
package org.onehippo.cm.model.tree;

/**
 * Represents the multiplicity of a ModelProperty and whether ordering is significant as an enum.
 */
public enum PropertyKind {

    /**
     * A single-valued property.
     */
    SINGLE,

    /**
     * A multi-valued property with significant ordering of items.
     */
    LIST,

    /**
     * A multi-valued property with insignificant ordering of items.
     */
    SET;

    /**
     * @return true iff this type indicates a multi-valued property
     */
    public boolean isMultiple() {
        return this != SINGLE;
    }

    public final String toString() {
        return name().toLowerCase();
    }

}
