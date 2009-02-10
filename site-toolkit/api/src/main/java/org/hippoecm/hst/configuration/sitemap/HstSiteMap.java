package org.hippoecm.hst.configuration.sitemap;

import java.util.List;

import org.hippoecm.hst.configuration.HstSite;

public interface HstSiteMap {
    
    HstSite getSite();
    
    List<HstSiteMapItem> getSiteMapItems();

    HstSiteMapItem getSiteMapItem(String value);
    
    HstSiteMapItem getSiteMapItemById(String id);
    
  }
