/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types;

/**
 * @version "$Id: NodeType.java 171483 2013-07-24 09:26:52Z mmilicevic $"
 */
public enum NodeType {


    SITEMAP_ITEM("hst:sitemenuitem");
    private final String primaryType;

    NodeType(final String primaryType) {
        this.primaryType = primaryType;
    }

    public String getPrimaryType() {
        return primaryType;
    }
}
