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
package org.onehippo.cm.engine.mapper;

import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Value;

/**
 * Default (fallback) file mapper. Uses property name as filename
 */
public class DefaultFileMapper extends AbstractFileMapper {

    @Override
    public String apply(Value value) {
        final DefinitionProperty property = value.getParent();
        final String propertyPath = property.getPath();
        final String filePath = constructFilePathFromJcrPath(propertyPath);
        return String.format("%s.%s", filePath, getFileExtension(property.getParent()));
    }
}
