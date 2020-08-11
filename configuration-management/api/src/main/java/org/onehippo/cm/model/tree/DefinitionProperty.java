/*
 *  Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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
 * Represents the (potential) state of a JCR Property as specified in a DefinitionItem tree.
 */
public interface DefinitionProperty extends DefinitionItem, ModelProperty {

    /**
     * @return the operation to be performed when merging this definition with previously-existing definitions
     */
    PropertyOperation getOperation();

    /**
     * @return true iff this definition represents a delete of the described property
     */
    boolean isDeleted();

    /**
     * @return true if {@link #isAddNewSystemValues()} is based on a set value
     */
    boolean isAddNewSystemValuesSet();
}
