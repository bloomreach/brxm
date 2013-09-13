/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model;

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types.ConfigType;

/**
 * @version "$Id: ConfigNode.java 171565 2013-07-24 14:38:15Z mmilicevic $"
 */
public class ConfigNode {


    private final ConfigType type;
    private final String name;
    private List<ConfigNode> childNodes = new ArrayList<ConfigNode>();
    private List<HstConfigProperty> properties = new ArrayList<HstConfigProperty>();
    private ConfigNode parent;


    public ConfigNode(final ConfigType type, final String name) {
        this.type = type;
        this.name = name;
    }

    public void addChildNode(final ConfigNode child) {
        child.setParent(this);
        childNodes.add(child);
    }

    public void addProperty(final HstConfigProperty property) {
        properties.add(property);
    }

    public ConfigNode getParent() {
        return parent;
    }

    public void setParent(final ConfigNode parent) {
        this.parent = parent;
    }

    public List<ConfigNode> getChildNodes() {
        return childNodes;
    }

    public String getPrimaryType() {
        return type.getPrimaryType();
    }

    public ConfigType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<HstConfigProperty> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigNode{");
        sb.append("type=").append(type);
        sb.append(", name='").append(name).append('\'');
        sb.append(", childNodes=").append(childNodes);
        sb.append(", properties=").append(properties);
        sb.append('}');
        return sb.toString();
    }

    public String getPath() {
        if (parent == null) {
            return name;
        }
        return parent.getPath() + '/' + name;
    }


}
