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
package org.onehippo.cm.model.mapper;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.tree.DefinitionProperty;
import org.onehippo.cm.model.tree.Value;

/**
 * Default (fallback) file mapper. Uses property name as filename
 */
public class DefaultFileMapper extends AbstractFileMapper {

    @Override
    public String apply(Value value) {
        final DefinitionProperty property = value.getParent();
        final String filePath = constructFilePathFromJcrPath(property.getPath());
        final String fileExtension = getFileExtension(property.getParent());
        final String valueExtension = value.isResource() ? StringUtils.substringAfterLast(value.getString(), ".") : "";
        final String resourceExtension =
                DEFAULT_EXTENSION.equals(fileExtension) && !StringUtils.isEmpty(valueExtension) && valueExtension.indexOf('/') == -1
                ? valueExtension : fileExtension;
        return String.format("%s.%s", filePath, resourceExtension);
    }
}
