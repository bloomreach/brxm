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

import java.util.Collections;
import java.util.Map;

import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ConfigurationProperty;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

public class ConfigurationNodeImpl extends ConfigurationItemImpl implements ConfigurationNode {

    private Map<String, ConfigurationNode> nodes;
    private Map<String, ConfigurationProperty> properties;

    @Override
    public Map<String, ConfigurationNode> getNodes() {
        if (nodes == null) {
            return emptyMap();
        }
        return unmodifiableMap(nodes);
    }

    public void setNodes(final Map<String, ConfigurationNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public Map<String, ConfigurationProperty> getProperties() {
        if (properties == null) {
            return emptyMap();
        }
        return unmodifiableMap(properties);
    }

    public void setProperties(final Map<String, ConfigurationProperty> properties) {
        this.properties = properties;
    }
}
