package org.hippoecm.hst.configuration.sitemap;


public interface SiteMapItem extends BaseSiteMapItem{

    /**
     * get a SiteMapItemService child based on the urlpart
     * @param urlPartName
     * @return SiteMapItemService or null when not found
     */
    public SiteMapItem getChild(String urlPartName);
    
    /**
     * get the first SiteMapItemService that matches according the SiteMapItemMatcher
     * @param SiteMapItemMatcher siteMapItemMatcher
     * @return SiteMapItemService or null when not found
     */
    public SiteMapItem getChild(SiteMapItemMatcher siteMapItemMatcher);
    
}
