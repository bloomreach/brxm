/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.LinkedList;
import java.util.List;

/**
 * @version "$Id: PluginConfigProperty.java 171584 2013-07-24 16:21:10Z mmilicevic $"
 */
public class PluginConfigProperty implements ConfigProperty {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    private String propertyName;
    private List<String> propertyValues;



    public PluginConfigProperty(final String propertyName) {
        this.propertyName = propertyName;
    }

    public PluginConfigProperty(final String propertyName, final String propertyValue) {
        this.propertyName = propertyName;
        addValue(propertyValue);
    }

    @Override
    public void addValue(final String value) {
        if (propertyValues == null) {
            propertyValues = new LinkedList<>();
        }
        propertyValues.add(value);
    }

    @Override
    public String[] getPropertyValuesArray() {
        if (propertyValues == null) {
            return EMPTY_STRING_ARRAY;
        }
        return propertyValues.toArray(new String[propertyValues.size()]);
    }

    /**
     * Returns (first) string value or null if no values
     */
    @Override
    public String getStringValue() {
        if (propertyValues != null && propertyValues.size() > 0) {
            return propertyValues.get(0);
        }
        return null;

    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public List<String> getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValues(final List<String> propertyValues) {
        this.propertyValues = propertyValues;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginConfigProperty{");
        sb.append("propertyName='").append(propertyName).append('\'');
        sb.append(", propertyValues=").append(propertyValues);
        sb.append('}');
        return sb.toString();
    }
}
