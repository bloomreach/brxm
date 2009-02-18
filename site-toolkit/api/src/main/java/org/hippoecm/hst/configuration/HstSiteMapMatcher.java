package org.hippoecm.hst.configuration;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

public interface HstSiteMapMatcher {
    
    MatchResult match(String link,HstSite hstSite);
  
    public interface MatchResult {
        
        HstSiteMapItem getSiteMapItem();
        
        HstComponentConfiguration getCompontentConfiguration();
        
        String getRemainder();
        
    }
}


