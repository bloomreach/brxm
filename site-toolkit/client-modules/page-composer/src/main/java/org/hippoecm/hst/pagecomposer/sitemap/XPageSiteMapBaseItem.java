/*
 * Copyright 2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.sitemap;

public class XPageSiteMapBaseItem {

    private String name;

    private String pageTitle;

    private String pathInfo;

    private String absoluteJcrPath;


    public String getName() {
        return name;
    }


    public void setName(final String name) {
        this.name = name;
    }


    public String getPathInfo() {
        return pathInfo;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(final String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public void setPathInfo(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    /**
     * @return absolute jcr path to the handle node
     */
    public String getAbsoluteJcrPath() {
        return absoluteJcrPath;
    }

    public void setAbsoluteJcrPath(final String absoluteJcrPath) {
        this.absoluteJcrPath = absoluteJcrPath;
    }

}
