/*
 * Copyright 2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.sitemap;

public class XPageSiteMapShallowItem extends XPageSiteMapBaseItem {

    boolean expandable;

    public XPageSiteMapShallowItem(final XPageSiteMapTreeItem source) {
        setName(source.getName());
        setPageTitle(source.getPageTitle());
        setPathInfo(source.getPathInfo());
        setAbsoluteJcrPath(source.getAbsoluteJcrPath());
    }

    public XPageSiteMapShallowItem(final String pathInfo) {
        setPathInfo(pathInfo);
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(final boolean expandable) {
        this.expandable = expandable;
    }
}
