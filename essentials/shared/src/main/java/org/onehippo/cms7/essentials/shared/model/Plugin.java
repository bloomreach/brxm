/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.shared.model;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @version "$Id: Plugin.java 157477 2013-03-08 14:50:29Z mmilicevic $"
 */
@XmlRootElement(name = "plugin")
public class Plugin {

    public String id;
    public String name;

    public boolean installed;
    public Vendor vendor;
    public String description;
    public String version;
    public String icon;


    private List<Version> versions;




    public Plugin() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public boolean getInstalled() {
        return installed;
    }

    public void setInstalled(final boolean installed) {
        this.installed = installed;
    }

    public void addVersion(final Version version) {
        if (versions == null) {
            versions = new LinkedList<Version>();
        }
        versions.add(version);
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @XmlElementWrapper(name = "versions")
    @XmlElement(name = "version")
    public List<Version> getVersions() {
        return versions;
    }

    public void setVersions(final List<Version> versions) {
        this.versions = versions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(final Vendor vendor) {
        this.vendor = vendor;
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }


    public boolean isInstalled() {
        return installed;
    }

    public String getIcon() {

        return icon;
    }

    public void setIcon(final String icon) {
        this.icon = icon;
    }
}
