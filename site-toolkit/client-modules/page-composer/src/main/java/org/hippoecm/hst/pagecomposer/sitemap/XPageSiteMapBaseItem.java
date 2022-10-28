/*
 * Copyright 2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.sitemap;

public class XPageSiteMapBaseItem {

    private String pathInfo;

    private String absoluteJcrPath;

    public String getPathInfo() {
        return pathInfo;
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
