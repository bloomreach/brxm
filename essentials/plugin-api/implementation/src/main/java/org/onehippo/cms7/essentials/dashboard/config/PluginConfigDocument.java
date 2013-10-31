/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: PluginConfigDocument.java 171585 2013-07-24 16:57:57Z mmilicevic $"
 */
public class PluginConfigDocument implements ConfigDocument {

    private static Logger log = LoggerFactory.getLogger(PluginConfigDocument.class);
    private final String name;
    private final List<ConfigProperty> properties = new LinkedList<>();


    public PluginConfigDocument(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addProperty(final ConfigProperty property) {
        properties.add(property);
    }

    @Override
    public List<ConfigProperty> getProperties() {
        return properties;
    }

    @Override
    public String getValue(final String name) {
        for (ConfigProperty property : properties) {
            if (property.getPropertyName().equals(name)) {
                return property.getStringValue();
            }
        }
        return null;
    }

    @Override
    public List<String> getValues(final String name) {
        for (ConfigProperty property : properties) {
            if (property.getPropertyName().equals(name)) {
                return property.getPropertyValues();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginConfigDocument{");
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
