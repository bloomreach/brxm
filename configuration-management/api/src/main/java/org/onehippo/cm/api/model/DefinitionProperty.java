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
package org.onehippo.cm.api.model;

public interface DefinitionProperty extends DefinitionItem {

    PropertyType getType();
    ValueType getValueType();

    /**
     * @throws ValueFormatException if the property is multi-valued.
     */
    Value getValue() throws ValueFormatException;

    /**
     * @throws ValueFormatException if the property is single-valued.
     */
    Value[] getValues() throws ValueFormatException;

    PropertyOperation getOperation();
}
