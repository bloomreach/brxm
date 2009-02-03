package org.hippoecm.hst.configuration;

import org.hippoecm.hst.configuration.components.HstComponents;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.service.Service;

public interface HstSite extends Service{

    public String getName();
    
    public String getContentPath();
    
    public HstSiteMap getSiteMap();
    
    public HstComponents getComponents();
}
