package org.hippoecm.hst.configuration;

import java.util.Map;

public interface HstSites {
    
    String getSitesContentPath();

    Map<String, HstSite> getSites();

    HstSite getSite(String name);
}