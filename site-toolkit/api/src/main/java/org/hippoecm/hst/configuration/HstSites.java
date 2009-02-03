package org.hippoecm.hst.configuration;

import java.util.Map;

public interface HstSites {
    public String getSitesContentPath();
    public Map<String,HstSite> getSites();
    public HstSite getSite(String name);
}