package org.hippoecm.hst.configuration.sitemap;

public interface HstSiteMapItemMatcher {

    /**
     * @param siteMapItemService
     * @return true when the SiteMapItemService matches this SiteMapItemMatcher
     */
    public boolean matches(HstSiteMapItem siteMapItemService);
}
