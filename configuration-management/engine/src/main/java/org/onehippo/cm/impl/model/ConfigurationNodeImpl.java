package org.onehippo.cm.impl.model;

import java.util.Map;

import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ConfigurationProperty;

public class ConfigurationNodeImpl extends ConfigurationItemImpl implements ConfigurationNode {

    private Map<String, ConfigurationNode> nodes;
    private Map<String, ConfigurationProperty> properties;

    @Override
    public Map<String, ConfigurationNode> getNodes() {
        return nodes;
    }

    public void setNodes(final Map<String, ConfigurationNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public Map<String, ConfigurationProperty> getProperties() {
        return properties;
    }

    public void setProperties(final Map<String, ConfigurationProperty> properties) {
        this.properties = properties;
    }
}
