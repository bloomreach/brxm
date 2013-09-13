/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.shared.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: Version.java 157469 2013-03-08 14:03:30Z mmilicevic $"
 */
@XmlRootElement(name = "version")
public class Version {

    private static Logger log = LoggerFactory.getLogger(Version.class);
    private String version;
    private List<Dependency> dependencies;


    public Version(final String version) {
        this.version = version;
    }

    public Version() {
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(final List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }


    public void addDependency(final Dependency dependency) {
        if (dependencies == null) {
            dependencies = new ArrayList<Dependency>();
        }
        dependencies.add(dependency);

    }





    @XmlAttribute
    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Version{");
        sb.append("version='").append(version).append('\'');
        sb.append('}');
        return sb.toString();
    }


}
