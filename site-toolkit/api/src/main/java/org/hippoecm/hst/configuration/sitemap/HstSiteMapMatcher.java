package org.hippoecm.hst.configuration.sitemap;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public interface HstSiteMapMatcher {
    
     MatchResult match(String path,HstSite hstSite);
    
    public interface MatchResult {
        
        HstSiteMapItem getSiteMapItem();
        
        HstComponentConfiguration getCompontentConfiguration();
        
        String getRemainder();
        
    }
}


