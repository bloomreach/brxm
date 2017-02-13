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
package org.onehippo.cm.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ConfigurationProperty;

public class ConfigurationNodeImpl extends ConfigurationItemImpl implements ConfigurationNode {

    private final Map<String, ConfigurationNodeImpl> modifiableNodes = new LinkedHashMap<>();
    private final Map<String, ConfigurationNode> nodes = Collections.unmodifiableMap(modifiableNodes);
    private final Map<String, ConfigurationPropertyImpl> modifiableProperties = new LinkedHashMap<>();
    private final Map<String, ConfigurationProperty> properties = Collections.unmodifiableMap(modifiableProperties);

    @Override
    public Map<String, ConfigurationNode> getNodes() {
        return nodes;
    }

    public Map<String, ConfigurationNodeImpl> getModifiableNodes() {
        return modifiableNodes;
    }

    public void addNode(final String name, final ConfigurationNodeImpl node) {
        modifiableNodes.put(name, node);
    }

    public void removeNode(final String name) {
        modifiableNodes.remove(name);
    }

    public void clearNodes() {
        modifiableNodes.clear();
    }

    @Override
    public Map<String, ConfigurationProperty> getProperties() {
        return properties;
    }

    public Map<String, ConfigurationPropertyImpl> getModifiableProperties() {
        return modifiableProperties;
    }

    public void addProperty(final String name, final ConfigurationPropertyImpl property) {
        modifiableProperties.put(name, property);
    }

    public void clearProperties() {
        modifiableProperties.clear();
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
    }
}
