package org.hippoecm.hst.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

public class HstSitesService implements HstSites{

    private Map<String, HstSite> hstSites = new HashMap<String, HstSite>();
    
    public HstSitesService(Node node) {
        init(node);
    }
    
    private void init(Node node) {
       
    }

    public HstSite getSite(String name) {
        return getSites().get(name);
    }

    public Map<String, HstSite> getSites() {
        return hstSites;
    }

    public String getSitesContentPath() {
        return null;
    }
    
    
}
