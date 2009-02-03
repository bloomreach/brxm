package org.hippoecm.hst.configuration;

import org.hippoecm.hst.configuration.pagemapping.PageMapping;
import org.hippoecm.hst.configuration.sitemap.SiteMapService;
import org.hippoecm.hst.service.Service;

public interface HstConfigurationService extends Service{

    public SiteMapService getSiteMapService();
    
    public PageMapping getPageMappingService();
}
