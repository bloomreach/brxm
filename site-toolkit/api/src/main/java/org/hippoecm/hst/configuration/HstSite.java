package org.hippoecm.hst.configuration;

import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;

public interface HstSite {

    public String getName();
    
    public String getContentPath();

    public HstSiteMap getSiteMap();
    
    public HstSites getHstSites();
    
    public HstComponentsConfiguration getComponents();
}
