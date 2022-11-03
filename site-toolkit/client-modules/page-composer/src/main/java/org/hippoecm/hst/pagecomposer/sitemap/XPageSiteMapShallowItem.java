/*
 * Copyright 2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.sitemap;

public class XPageSiteMapShallowItem extends XPageSiteMapBaseItem {

    boolean expandable;

    XPageSiteMapTreeItem parent;

    public XPageSiteMapShallowItem(final XPageSiteMapTreeItem source) {
        setName(source.getName());
        setPageTitle(source.getPageTitle());
        setPathInfo(source.getPathInfo());
        setAbsoluteJcrPath(source.getAbsoluteJcrPath());
        parent = source.getParent();
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

    public XPageSiteMapTreeItem getParent() {
        return parent;
    }
}
