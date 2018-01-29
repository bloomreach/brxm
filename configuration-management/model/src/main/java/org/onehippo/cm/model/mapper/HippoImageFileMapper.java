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

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.DefinitionNode;
import org.onehippo.cm.model.tree.DefinitionProperty;
import org.onehippo.cm.model.tree.PropertyKind;
import org.onehippo.cm.model.tree.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File mapper for hippogallery:image & hippogallery:imageset node types
 *
 * TODO: move into CMS (gallery functionality) and get rid of duplicate constant definitions
 */
public class HippoImageFileMapper extends AbstractFileMapper {

    private static final Logger logger = LoggerFactory.getLogger(HippoImageFileMapper.class);

    static final JcrPathSegment HIPPOGALLERY_IMAGE = JcrPaths.getSegment("hippogallery:image");
    static final JcrPathSegment HIPPOGALLERY_IMAGESET = JcrPaths.getSegment("hippogallery:imageset");
    private static final JcrPathSegment HIPPOGALLERY_FILENAME = JcrPaths.getSegment("hippogallery:filename");

    private static final String IMAGE_NAME_ARRAY_PATTERN = "%s[%s].%s";
    private static final String GALLERY_IMAGE_ARRAY_NAME_PATTERN = "%s_" + IMAGE_NAME_ARRAY_PATTERN;

    private static final String IMAGE_NAME_PATTERN = "%s.%s";
    private static final String GALLERY_IMAGE_NAME_PATTERN = "%s_" + IMAGE_NAME_PATTERN;

    @Override
    public String apply(Value value) {

        try {
            final DefinitionProperty property = value.getParent();
            final DefinitionNode imageNode = property.getParent();

            if (!isType(imageNode, HIPPOGALLERY_IMAGE)) {
                return null;
            }

            final Optional<Integer> arrayIndex = calculateArrayIndex(property, value);

            final DefinitionNode imageSetNode = imageNode.getParent();
            if (isType(imageSetNode, HIPPOGALLERY_IMAGESET)) {
                final DefinitionProperty fileNameProperty = imageSetNode.getProperty(HIPPOGALLERY_FILENAME);
                final String fileName = fileNameProperty != null ? fileNameProperty.getValue().getString() : mapNodeNameToFileName(imageSetNode.getName());
                final String baseName = StringUtils.substringBeforeLast(fileName, DOT_SEPARATOR);
                final String suffix = mapNodeNameToFileName(imageNode.getName());
                final String extension = fileName.contains(DOT_SEPARATOR) ? StringUtils.substringAfterLast(fileName, DOT_SEPARATOR) : getFileExtension(imageNode);
                final String finalName = arrayIndex.map(integer -> String.format(GALLERY_IMAGE_ARRAY_NAME_PATTERN, baseName, suffix, integer, extension))
                        .orElseGet(() -> String.format(GALLERY_IMAGE_NAME_PATTERN, baseName, suffix, extension));
                return String.format("%s/%s", constructFilePathFromJcrPath(imageSetNode.getParent().getPath()), finalName);
            } else {
                final String folderPath = constructFilePathFromJcrPath(imageNode.getPath());
                final String name = mapNodeNameToFileName(imageNode.getName());
                final String extension = getFileExtension(imageNode);
                final String finalName = arrayIndex.map(integer -> String.format(IMAGE_NAME_ARRAY_PATTERN, name, integer, extension))
                        .orElseGet(() -> String.format(IMAGE_NAME_PATTERN, name, extension));
                return String.format("%s/%s", folderPath, finalName);
            }
        } catch (Exception e) {
            logger.error("HippoImageFileMapper failed", e);
            return null;
        }
    }

    private Optional<Integer> calculateArrayIndex(DefinitionProperty property, Value value) {
        if (property.isMultiple()) {
            return Optional.of(property.getValues().indexOf(value));
        }
        return Optional.empty();
    }

}
