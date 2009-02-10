package org.hippoecm.hst.configuration;

import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;

public interface HstSite {

    String getName();

    String getContentPath();

    HstComponentsConfiguration getComponents();

    HstSiteMap getSiteMap();

    HstSites getHstSites();

}
