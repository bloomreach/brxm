package org.onehippo.cms7.essentials.dashboard.wiki.model;

import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public abstract class TestHippoNode {

    private static Logger log = LoggerFactory.getLogger(TestHippoNode.class);

    @JcrPath
    protected String path;

    @JcrName
    protected String name;

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
