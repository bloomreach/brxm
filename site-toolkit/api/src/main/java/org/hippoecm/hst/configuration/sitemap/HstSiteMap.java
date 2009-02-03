package org.hippoecm.hst.configuration.sitemap;

import org.hippoecm.hst.service.Service;


public interface HstSiteMap extends Service{
    
    public HstMatchingSiteMapItem match(String url);
   
    
}
