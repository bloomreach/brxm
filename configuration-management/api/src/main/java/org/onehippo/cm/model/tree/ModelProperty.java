/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

/**
 * Represents the (potential) state of a JCR Property as specified in either a ConfigurationItem or
 * DefinitionItem tree.
 */
public interface ModelProperty extends ModelItem {

    /**
     * @return the JCR data type for this property as a ValueType enum
     */
    ValueType getValueType();

    /**
     * @return the type of this property: SINGLE, ordered-LIST, or unordered-SET
     */
    PropertyKind getKind();

    /**
     * @return true iff this is a multi-valued property
     */
    boolean isMultiple();

    /**
     * @return the Value of this property
     * @throws ValueFormatException if the property is multi-valued.
     */
    Value getValue() throws ValueFormatException;

    /**
     * @return the Values of this property
     * @throws ValueFormatException if the property is single-valued.
     */
    List<? extends Value> getValues() throws ValueFormatException;
}
