package org.hippoecm.hst.configuration;

import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;

public interface HstSite {

    String getName();

    String getContentPath();

    HstComponentsConfiguration getComponentsConfiguration();

    HstSiteMap getSiteMap();

    HstSites getHstSites();
    
    LocationMapTree getLocationMap();

}
