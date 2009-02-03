package org.hippoecm.hst.configuration.sitemap;

import org.hippoecm.hst.service.Service;


public interface SiteMapService extends Service{
    
    public MatchingSiteMapItem match(String url);
   
    
}
