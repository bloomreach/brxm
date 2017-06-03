/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.model.util;

import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationModelUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationModelUtils.class);
    private static final String SEPARATOR = "/";

    private ConfigurationModelUtils() { }

    /**
     * Determine the category of a node at the specified absolute path.
     *
     * @param absoluteNodePath absolute path to node
     * @param model            configuration model to check against
     * @return                 category of the node pointed to
     */
    public static ConfigurationItemCategory getCategoryForNode(final String absoluteNodePath, final ConfigurationModel model) {
        return getCategoryForItem(absoluteNodePath, false, model);
    }

    /**
     * Determine the category of a property at the specified absolute path.
     *
     * @param absolutePropertyPath absolute path to property
     * @param model                configuration model to check against
     * @return                     category of the property pointed to
     */
    public static ConfigurationItemCategory getCategoryForProperty(final String absolutePropertyPath, final ConfigurationModel model) {
        return getCategoryForItem(absolutePropertyPath, true, model);
    }

    public static ConfigurationItemCategory getCategoryForItem(final String absoluteItemPath, final boolean propertyPath, ConfigurationModel model) {
        if (absoluteItemPath.equals(SEPARATOR)) {
            return ConfigurationItemCategory.CONFIG; // special treatment for root node
        }

        if (!absoluteItemPath.startsWith(SEPARATOR)) {
            logger.warn("{} is not a valid absolute path");
            return ConfigurationItemCategory.CONFIG;
        }

        final String[] pathSegments = absoluteItemPath.substring(1).split(SEPARATOR);

        ConfigurationNode modelNode = model.getConfigurationRootNode();
        for (int i = 0; i < pathSegments.length; i++) {
            final String childName = pathSegments[i];
            if (i == pathSegments.length-1) {
                return propertyPath
                        ? modelNode.getChildPropertyCategory(childName)
                        : modelNode.getChildNodeCategory(SnsUtils.createIndexedName(childName));
            } else {
                final String indexedChildName = SnsUtils.createIndexedName(childName);
                if (modelNode.getNodes().containsKey(indexedChildName)) {
                    modelNode = modelNode.getNodes().get(indexedChildName);
                } else {
                    return modelNode.getChildNodeCategory(indexedChildName);
                }
            }
        }
        // will never reach this but compiler needs to be kept happy
        throw new IllegalStateException("unexpected");
    }
}
