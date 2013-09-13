/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types;

/**
 * * @version "$Id: ConfigType.java 171483 2013-07-24 09:26:52Z mmilicevic $"
 */
public enum ConfigType {

    SITEMAP("hst:sitemap", "hst:sitemapitem"),
    MENU("hst:sitemenus", "hst:sitemenuitem"),
    PAGE("hst:pages", "hst:component"),
    COMPONENT("hst:components", "hst:component"),
    TEMPLATE("hst:templates", "hst:template"),
    CATALOG("hst:catalog", "hst:containeritempackage");
    private final String path;
    private final String primaryType;

    ConfigType(final String path, final String primaryType) {
        this.path = path;
        this.primaryType = primaryType;
    }

    public String getPath() {
        return path;
    }

    public String getPrimaryType() {
        return primaryType;
    }
}
