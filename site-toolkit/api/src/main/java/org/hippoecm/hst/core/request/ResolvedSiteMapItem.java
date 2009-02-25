package org.hippoecm.hst.core.request;

import org.hippoecm.hst.configuration.sitemap.HstSiteMap;


public interface ResolvedSiteMapItem {
    
    /**
     * Returns a property from the siteMapItem configuration but possible variables ( $1 or $2 etc ) replaced with the current value
     * 
     * @param name
     * @return property Object 
     */
    Object getProperty(String name);
    
    HstSiteMap getHstSiteMap();
}
