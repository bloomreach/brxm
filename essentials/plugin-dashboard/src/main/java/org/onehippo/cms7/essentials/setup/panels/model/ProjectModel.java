package org.onehippo.cms7.essentials.setup.panels.model;

import java.io.Serializable;

/**
 * User: obourgeois
 * Date: 29-10-13
 */
public class ProjectModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}