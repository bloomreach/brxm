/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.ConfigurationNode;
import org.onehippo.cm.model.util.SnsUtils;

public class ConfigurationNodeImpl extends ConfigurationItemImpl<DefinitionNodeImpl>
        implements ConfigurationNode {

    // Nodes names must always be indexed names, e.g. node[1]
    private final Map<String, ConfigurationNodeImpl> modifiableNodes = new LinkedHashMap<>();
    private final Map<String, ConfigurationPropertyImpl> modifiableProperties = new LinkedHashMap<>();

    private final Collection<ConfigurationNodeImpl> nodes = Collections.unmodifiableCollection(modifiableNodes.values());
    private final Collection<ConfigurationPropertyImpl> properties = Collections.unmodifiableCollection(modifiableProperties.values());

    private Boolean ignoreReorderedChildren;

    // Category settings are not supported for individual same-name siblings. When configuring a certain node name
    // with a category, all SNS have that category, hence childNodeCategorySettings does not use indexed names.
    private Map<String, Pair<ConfigurationItemCategory, DefinitionItemImpl>> childNodeCategorySettings = new HashMap<>();
    private Map<String, Pair<ConfigurationItemCategory, DefinitionItemImpl>> childPropertyCategorySettings = new HashMap<>();
    private ConfigurationItemCategory residualNodeCategory;

    @Override
    public void setName(final String name) {
        super.setName(JcrPaths.getSegment(name).forceIndex());
    }

    @Override
    public void setName(final JcrPathSegment name) {
        super.setName(name.forceIndex());
    }

    @Override
    public Collection<ConfigurationNodeImpl> getNodes() {
        return nodes;
    }

    public Set<String> getNodeNames() {
        return modifiableNodes.keySet();
    }

    public ConfigurationNodeImpl getNode(final String name) {
        return getNode(JcrPaths.getSegment(name));
    }

    @Override
    public ConfigurationNodeImpl getNode(final JcrPathSegment name) {
        return modifiableNodes.get(name.forceIndex().toString());
    }

    public void addNode(final String name, final ConfigurationNodeImpl node) {
        modifiableNodes.put(JcrPaths.getSegment(name).forceIndex().toString(), node);
    }

    public void orderBefore(final String srcChildName, final String destChildName) {
        if (!modifiableNodes.containsKey(srcChildName)) {
            final String msg = String.format("Node '%s' has no child named '%s'.", getJcrPath(), srcChildName);
            throw new IllegalArgumentException(msg);
        }
        if (!modifiableNodes.containsKey(destChildName)) {
            final String msg = String.format("Node '%s' has no child named '%s'.", getJcrPath(), destChildName);
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

    @Override
    public Collection<ConfigurationPropertyImpl> getProperties() {
        return properties;
    }

    public Set<String> getPropertyNames() {
        return modifiableProperties.keySet();
    }

    @Override
    public ConfigurationPropertyImpl getProperty(final JcrPathSegment name) {
        return getProperty(name.toString());
    }

    @Override
    public ConfigurationPropertyImpl getProperty(final String name) {
        return modifiableProperties.get(name);
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

    /**
     * @return true iff no nodes or normal JCR properties are defined here; note that this excludes from consideration
     *     any .meta properties, such as .meta:delete or category-related properties
     */
    public boolean hasNoJcrNodesOrProperties() {
        return modifiableNodes.isEmpty() && modifiableProperties.isEmpty();
    }

    @Override
    public Boolean getIgnoreReorderedChildren() {
        return ignoreReorderedChildren;
    }

    public void setIgnoreReorderedChildren(final boolean ignoreReorderedChildren) {
        this.ignoreReorderedChildren = ignoreReorderedChildren;
    }

    protected ConfigurationItemCategory getChildNodeCategory(final String indexedNodeName) {
        return getChildNodeCategory(JcrPaths.getSegment(indexedNodeName), null);
    }


    @Override
    public ConfigurationItemCategory getChildNodeCategory(final JcrPathSegment indexedNodeName) {
        return getChildNodeCategory(indexedNodeName, null);
    }

    @Override
    public ConfigurationItemCategory getChildNodeCategory(final JcrPathSegment indexedNodeName,
                                                          final ConfigurationItemCategory residualNodeCategoryOverride) {
        final ConfigurationItemCategory effectiveResidualNodeCategory
                = residualNodeCategoryOverride != null ? residualNodeCategoryOverride : residualNodeCategory;

        if (modifiableNodes.containsKey(indexedNodeName.forceIndex().toString())) {
            return ConfigurationItemCategory.CONFIG;
        }

        final String unindexedName = indexedNodeName.getName();
        if (childNodeCategorySettings.containsKey(unindexedName)) {
            return childNodeCategorySettings.get(unindexedName).getLeft();
        }

        if (effectiveResidualNodeCategory != null) {
            return effectiveResidualNodeCategory;
        }

        return ConfigurationItemCategory.CONFIG;
    }

    public ConfigurationItemCategory getChildPropertyCategory(final String propertyName) {
        return getChildPropertyCategory(JcrPaths.getSegment(propertyName));
    }

    @Override
    public ConfigurationItemCategory getChildPropertyCategory(final JcrPathSegment propertyName) {
        if (childPropertyCategorySettings.containsKey(propertyName.toString())) {
            return childPropertyCategorySettings.get(propertyName.toString()).getLeft();
        }

        return ConfigurationItemCategory.CONFIG;
    }

    public Pair<ConfigurationItemCategory, DefinitionItemImpl> getChildNodeCategorySettings(final String name) {
        return childNodeCategorySettings.get(SnsUtils.getUnindexedName(name));
    }

    public void setChildNodeCategorySettings(final String name, final ConfigurationItemCategory category, final DefinitionItemImpl definitionItem) {
        childNodeCategorySettings.put(SnsUtils.getUnindexedName(name), Pair.of(category, definitionItem));
    }

    public Pair<ConfigurationItemCategory, DefinitionItemImpl> clearChildNodeCategorySettings(final String name) {
        return childNodeCategorySettings.remove(SnsUtils.getUnindexedName(name));
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
