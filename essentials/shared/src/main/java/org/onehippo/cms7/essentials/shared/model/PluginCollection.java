/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.shared.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: PluginCollection.java 157419 2013-03-08 10:16:35Z mmilicevic $"
 */
@XmlRootElement(name = "hippo-plugins")
public class PluginCollection {

    private static Logger log = LoggerFactory.getLogger(PluginCollection.class);

    private String description;

    private List<Plugin> plugins;

    public PluginCollection() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void addPlugin(final Plugin plugin){
        if(plugins ==null){
            plugins = new ArrayList<Plugin>();
        }
        plugins.add(plugin);
    }

    @XmlElementWrapper(name = "plugins")
    @XmlElement(name = "plugin")
    public List<Plugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(final List<Plugin> plugins) {
        this.plugins = plugins;
    }
}
