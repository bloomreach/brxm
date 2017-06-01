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

import java.util.Arrays;
import java.util.Iterator;

import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.ConfigurationNode;
import org.onehippo.cm.model.SnsUtils;
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
        if (absoluteNodePath.equals(SEPARATOR)) {
            return ConfigurationItemCategory.CONFIGURATION; // special treatment for root node
        }

        if (!absoluteNodePath.startsWith(SEPARATOR)) {
            logger.warn("{} is not a valid absolute path to a node");
            return ConfigurationItemCategory.CONFIGURATION;
        }

        final String[] pathElements = absoluteNodePath.split(SEPARATOR);
        final int skipRoot = 1;
        final Iterator<String> nodeNameIterator = Arrays.stream(pathElements, skipRoot, pathElements.length).iterator();

        ConfigurationNode modelNode = model.getConfigurationRootNode();
        while (nodeNameIterator.hasNext()) {
            final String childName = nodeNameIterator.next();
            final String indexedChildName = SnsUtils.createIndexedName(childName);
            if (!modelNode.getNodes().containsKey(indexedChildName)) {
                return modelNode.getChildNodeCategory(indexedChildName);
            }
            modelNode = modelNode.getNodes().get(indexedChildName);
        }
        return ConfigurationItemCategory.CONFIGURATION;
    }
}
