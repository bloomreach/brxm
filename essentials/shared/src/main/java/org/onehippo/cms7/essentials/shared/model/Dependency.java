/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.shared.model;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: Dependency.java 157508 2013-03-10 13:02:32Z fvlankvelt $"
 */
@XmlRootElement(name="dependency")
public class Dependency {

    private static Logger log = LoggerFactory.getLogger(Dependency.class);

    /**
     * Project type..e.g HST, CONTENT, CMS
     */
    private String projectType = "CMS";
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;


    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(final String projectType) {
        this.projectType = projectType;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }
}
