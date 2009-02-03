package org.hippoecm.hst.configuration.sitemap;


public interface HstSiteMapItem extends HstBaseSiteMapItem{

    /**
     * get a SiteMapItemService child based on the urlpart
     * @param urlPartName
     * @return SiteMapItemService or null when not found
     */
    public HstSiteMapItem getChild(String urlPartName);
    
    /**
     * get the first SiteMapItemService that matches according the SiteMapItemMatcher
     * @param HstSiteMapItemMatcher siteMapItemMatcher
     * @return SiteMapItemService or null when not found
     */
    public HstSiteMapItem getChild(HstSiteMapItemMatcher siteMapItemMatcher);
    
}
