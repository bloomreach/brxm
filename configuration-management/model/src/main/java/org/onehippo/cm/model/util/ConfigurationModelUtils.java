/*
 * Copyright 2017,2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.function.Function;

import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: IMHO, these should be public instance methods on ConfigurationModelImpl
public class ConfigurationModelUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationModelUtils.class);

    private ConfigurationModelUtils() { }

    /**
     * Determine the category of a node at the specified absolute path.
     *
     * @param absoluteNodePath absolute path to node
     * @param model            configuration model to check against
     * @return                 category of the node pointed to
     */
    public static ConfigurationItemCategory getCategoryForNode(final String absoluteNodePath,
                                                               final ConfigurationModel model) {
        return getCategoryForItem(absoluteNodePath, false, model);
    }

    /**
     * Determine the category of a property at the specified absolute path.
     *
     * @param absolutePropertyPath absolute path to property
     * @param model                configuration model to check against
     * @return                     category of the property pointed to
     */
    public static ConfigurationItemCategory getCategoryForProperty(final String absolutePropertyPath,
                                                                   final ConfigurationModel model) {
        return getCategoryForItem(absolutePropertyPath, true, model);
    }

    /**
     * Determine the category of an item at the specified absolute path.
     *
     * @param absoluteItemPath absolute path a an item
     * @param propertyPath     indicates whether the item is a node or property
     * @param model            configuration model to check against
     * @return                 category of the property pointed to
     */
    public static ConfigurationItemCategory getCategoryForItem(final String absoluteItemPath,
                                                               final boolean propertyPath,
                                                               final ConfigurationModel model) {
        return getCategoryForItem(absoluteItemPath, propertyPath, model, (path) -> null);
    }

    /**
     * Determine the category of an item at the specified absolute path, taking category overrides for specific node
     * paths into account as well.
     *
     * @param absoluteItemPath                     absolute path a an item
     * @param propertyPath                         indicates whether the item is a node or property
     * @param model                                configuration model to check against
     * @param residualNodeCategoryOverrideResolver function returning the .meta:residual-node-type-category override
     *                                             for a given absolute node path, or null if no override is specified
     * @return
     */
    public static ConfigurationItemCategory getCategoryForItem(
            final String absoluteItemPath,
            final boolean propertyPath,
            final ConfigurationModel model,
            final Function<String, ConfigurationItemCategory> residualNodeCategoryOverrideResolver) {

        final JcrPath itemPath = JcrPaths.getPath(absoluteItemPath);
        if (itemPath.isRoot()) {
            return ConfigurationItemCategory.CONFIG; // special treatment for root node
        }

        if (!itemPath.isAbsolute()) {
            logger.warn("{} is not a valid absolute path", absoluteItemPath);
            return ConfigurationItemCategory.CONFIG;
        }

        JcrPath parent = JcrPaths.ROOT;
        ConfigurationNode modelNode = model.getConfigurationRootNode();
        for (int i = 0; i < itemPath.getSegmentCount(); i++) {
            final JcrPathSegment childSegment = itemPath.getSegment(i);
            final String childName = childSegment.toString();
            final String indexedChildName = childSegment.forceIndex().toString();
            if (i == itemPath.getSegmentCount() - 1) {
                final ConfigurationItemCategory override = residualNodeCategoryOverrideResolver.apply(parent.toString());
                return propertyPath
                        ? modelNode.getChildPropertyCategory(childName)
                        : modelNode.getChildNodeCategory(indexedChildName, override);
            } else {
                if (modelNode.getNode(indexedChildName) != null) {
                    modelNode = modelNode.getNode(indexedChildName);
                } else {
                    final ConfigurationItemCategory override = residualNodeCategoryOverrideResolver.apply(parent.toString());
                    return modelNode.getChildNodeCategory(indexedChildName, override);
                }
            }
            parent = parent.resolve(childSegment);
        }
        // will never reach this but compiler needs to be kept happy
        throw new IllegalStateException("unexpected");
    }

}
