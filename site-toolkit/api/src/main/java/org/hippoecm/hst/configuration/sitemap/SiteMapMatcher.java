package org.hippoecm.hst.configuration.sitemap;

import org.hippoecm.hst.configuration.HstSite;

public interface SiteMapMatcher {
    
     MatchResult match(String path,HstSite hstSite);
    
    public interface MatchResult {
        
         HstSiteMapItem getSiteMapItem();
        
         String getRemainder();
        
    }
}


