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
package org.onehippo.cm.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.ConfigurationNode;
import org.onehippo.cm.model.SnsUtils;

public class ConfigurationNodeImpl extends ConfigurationItemImpl implements ConfigurationNode {

    // Nodes names must always be indexed names, e.g. node[1]
    private final Map<String, ConfigurationNodeImpl> modifiableNodes = new LinkedHashMap<>();
    private final Map<String, ConfigurationNodeImpl> unmodifiableMapWithModifiableNodes = Collections.unmodifiableMap(modifiableNodes);
    private final Map<String, ConfigurationNodeImpl> unmodifiableNodes = Collections.unmodifiableMap(modifiableNodes);

    private final Map<String, ConfigurationPropertyImpl> modifiableProperties = new LinkedHashMap<>();
    private final Map<String, ConfigurationPropertyImpl> unmodifiableMapWithModifiableProperties = Collections.unmodifiableMap(modifiableProperties);
    private final Map<String, ConfigurationPropertyImpl> unmodifiableProperties = Collections.unmodifiableMap(modifiableProperties);

    private Boolean ignoreReorderedChildren;

    // Category settings are not supported for individual same-name siblings, when configuring a certain node name
    // with a category, all SNS should have that category, hence childNodeCategorySettings does not use indexed names
    private Map<String, Pair<ConfigurationItemCategory, DefinitionItemImpl>> childNodeCategorySettings = new HashMap<>();
    private Map<String, Pair<ConfigurationItemCategory, DefinitionItemImpl>> childPropertyCategorySettings = new HashMap<>();
    private ConfigurationItemCategory residualNodeCategory;

    @Override
    public Map<String, ConfigurationNodeImpl> getNodes() {
        return unmodifiableNodes;
    }

    public Map<String, ConfigurationNodeImpl> getModifiableNodes() {
        return unmodifiableMapWithModifiableNodes;
    }

    public void addNode(final String name, final ConfigurationNodeImpl node) {
        modifiableNodes.put(name, node);
    }

    public void orderBefore(final String srcChildName, final String destChildName) {
        if (!modifiableNodes.containsKey(srcChildName)) {
            final String msg = String.format("Node '%s' has no child named '%s'.", getPath(), srcChildName);
            throw new IllegalArgumentException(msg);
        }
        if (!modifiableNodes.containsKey(destChildName)) {
            final String msg = String.format("Node '%s' has no child named '%s'.", getPath(), destChildName);
            throw new IllegalArgumentException(msg);
        }

        final List<String> toBeReinsertedChildren = new ArrayList<>();
        boolean destFound = false;

        for (String childName : modifiableNodes.keySet()) {
            if (childName.equals(destChildName)) {
                destFound = true;
            }
            if (destFound && !childName.equals(srcChildName)) {
                toBeReinsertedChildren.add(childName);
            }
        }

        modifiableNodes.put(srcChildName, modifiableNodes.remove(srcChildName));
        toBeReinsertedChildren.forEach(child -> modifiableNodes.put(child, modifiableNodes.remove(child)));

        if (SnsUtils.hasSns(srcChildName, modifiableNodes.keySet())) {
            updateSnsIndices(srcChildName);
        }
    }

    private void updateSnsIndices(final String indexedName) {
        final String unindexedName = SnsUtils.getUnindexedName(indexedName);
        final Map<String, ConfigurationNodeImpl> copy = new LinkedHashMap<>(modifiableNodes);
        modifiableNodes.clear();
        int index = 1;
        for (String sibling : copy.keySet()) {
            if (unindexedName.equals(SnsUtils.getUnindexedName(sibling))) {
                final String newName = SnsUtils.createIndexedName(unindexedName, index);
                final ConfigurationNodeImpl siblingNode = copy.get(sibling);
                siblingNode.setName(newName);
                modifiableNodes.put(newName, copy.get(sibling));
                index++;
            } else {
                modifiableNodes.put(sibling, copy.get(sibling));
            }
        }
    }

    public void removeNode(final String name, boolean updateSnsIndices) {
        modifiableNodes.remove(name);
        if (updateSnsIndices) {
            updateSnsIndices(name);
        }
    }

    public void clearNodes() {
        modifiableNodes.clear();
    }

    public boolean isNew() {
        return modifiableNodes.isEmpty() && modifiableProperties.isEmpty();
    }

    @Override
    public Map<String, ConfigurationPropertyImpl> getProperties() {
        return unmodifiableProperties;
    }

    public Map<String, ConfigurationPropertyImpl> getModifiableProperties() {
        return unmodifiableMapWithModifiableProperties;
    }

    public void addProperty(final String name, final ConfigurationPropertyImpl property) {
        modifiableProperties.put(name, property);
    }

    public void removeProperty(final String name) {
        modifiableProperties.remove(name);
    }

    public void clearProperties() {
        modifiableProperties.clear();
    }

    @Override
    public Boolean getIgnoreReorderedChildren() {
        return ignoreReorderedChildren;
    }

    public void setIgnoreReorderedChildren(final boolean ignoreReorderedChildren) {
        this.ignoreReorderedChildren = ignoreReorderedChildren;
    }

    @Override
    public ConfigurationItemCategory getChildNodeCategory(final String indexNodeName) {
        if (modifiableNodes.containsKey(indexNodeName)) {
            return ConfigurationItemCategory.CONFIGURATION;
        }

        final String unindexedName = SnsUtils.getUnindexedName(indexNodeName);
        if (childNodeCategorySettings.containsKey(unindexedName)) {
            return childNodeCategorySettings.get(unindexedName).getLeft();
        }

        if (residualNodeCategory != null) {
            return residualNodeCategory;
        }

        return ConfigurationItemCategory.CONFIGURATION;
    }

    @Override
    public ConfigurationItemCategory getChildPropertyCategory(final String propertyName) {
        if (childPropertyCategorySettings.containsKey(propertyName)) {
            return childPropertyCategorySettings.get(propertyName).getLeft();
        }

        return ConfigurationItemCategory.CONFIGURATION;
    }

    public Pair<ConfigurationItemCategory, DefinitionItemImpl> getChildNodeCategorySettings(final String name) {
        return childNodeCategorySettings.get(name);
    }

    public void setChildNodeCategorySettings(final String name, final ConfigurationItemCategory category, final DefinitionItemImpl definitionItem) {
        childNodeCategorySettings.put(SnsUtils.getUnindexedName(name), Pair.of(category, definitionItem));
    }

    public Pair<ConfigurationItemCategory, DefinitionItemImpl> clearChildNodeCategorySettings(final String name) {
        return childNodeCategorySettings.remove(name);
    }

    public Pair<ConfigurationItemCategory, DefinitionItemImpl> getChildPropertyCategorySettings(final String name) {
        return childPropertyCategorySettings.get(name);
    }

    public void setChildPropertyCategorySettings(final String name, final ConfigurationItemCategory category, final DefinitionItemImpl definitionItem) {
        childPropertyCategorySettings.put(name, Pair.of(category, definitionItem));
    }

    public Pair<ConfigurationItemCategory, DefinitionItemImpl> clearChildPropertyCategorySettings(final String name) {
        return childPropertyCategorySettings.remove(name);
    }

    public ConfigurationItemCategory getResidualNodeCategory() {
        return residualNodeCategory;
    }

    public void setResidualNodeCategory(final ConfigurationItemCategory residualNodeCategory) {
        this.residualNodeCategory = residualNodeCategory;
    }

}
