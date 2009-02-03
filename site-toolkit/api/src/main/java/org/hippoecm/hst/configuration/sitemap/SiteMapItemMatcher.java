package org.hippoecm.hst.configuration.sitemap;

public interface SiteMapItemMatcher {

    /**
     * @param siteMapItemService
     * @return true when the SiteMapItemService matches this SiteMapItemMatcher
     */
    public boolean matches(SiteMapItem siteMapItemService);
}
