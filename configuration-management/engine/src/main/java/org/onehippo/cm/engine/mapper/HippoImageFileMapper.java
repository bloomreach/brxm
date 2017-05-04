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

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Value;

import java.util.Arrays;
import java.util.Optional;

/**
 * File mapper for hippogallery:image & hippogallery:imageset node types
 */
public class HippoImageFileMapper extends AbstractFileMapper {

    public static final String IMAGE_NAME_ARRAY_PATTERN = "%s[%s].%s";
    public static final String GALLERY_IMAGE_ARRAY_NAME_PATTERN = "%s_" + IMAGE_NAME_ARRAY_PATTERN;

    public static final String IMAGE_NAME_PATTERN = "%s.%s";
    public static final String GALLERY_IMAGE_NAME_PATTERN = "%s_" + IMAGE_NAME_PATTERN;

    @Override
    public Optional<String> apply(Value value) {

        final DefinitionProperty property = value.getParent();
        final DefinitionNode node = property.getParent();

        if (!isType(node, HIPPOGALLERY_IMAGE)) {
            return Optional.empty();
        }

        final Optional<Integer> arrayIndex = calculateArrayIndex(value);

        final DefinitionNode imageSetNode = node.getParent();
        if (isType(imageSetNode, HIPPOGALLERY_IMAGESET)) {
            final DefinitionProperty fileProperty = imageSetNode.getProperties().get(HIPPOGALLERY_FILENAME);
            final String fileName = fileProperty != null ? fileProperty.getValue().getString() : normalizeJcrName(imageSetNode.getName());
            final String baseName = StringUtils.substringBeforeLast(fileName, DOT_SEPARATOR);
            final String suffix = normalizeJcrName(node.getName());
            final String extension = fileName.contains(DOT_SEPARATOR) ? StringUtils.substringAfterLast(fileName, DOT_SEPARATOR) : getFileExtension(node);
            final String finalName = arrayIndex.map(integer -> String.format(GALLERY_IMAGE_ARRAY_NAME_PATTERN, baseName, suffix, integer, extension))
                    .orElseGet(() -> String.format(GALLERY_IMAGE_NAME_PATTERN, baseName, suffix, extension));
            final String fullName = String.format("%s/%s", constructPathFromJcrPath(imageSetNode.getParent().getPath()), finalName);
            return Optional.of(fullName);
        } else {
            final String folderPath = constructPathFromJcrPath(node.getPath());
            final String name = normalizeJcrName(node.getName());
            final String extension = getFileExtension(node);
            final String finalName = arrayIndex.map(integer -> String.format(IMAGE_NAME_ARRAY_PATTERN, name, integer, extension))
                    .orElseGet(() -> String.format(IMAGE_NAME_PATTERN, name, extension));
            final String fullName = String.format("%s/%s", folderPath, finalName);
            return Optional.of(fullName);
        }
    }

    private Optional<Integer> calculateArrayIndex(Value value) {
        if (value.getParent().getType() == PropertyType.LIST || value.getParent().getType() == PropertyType.SET) {
            return Optional.of(Arrays.asList(value.getParent().getValues()).indexOf(value));
        }
        return Optional.empty();
    }
}
