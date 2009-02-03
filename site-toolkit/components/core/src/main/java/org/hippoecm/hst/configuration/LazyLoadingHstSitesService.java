package org.hippoecm.hst.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;


public class LazyLoadingHstSitesService extends HstSitesService implements AugmentableHstSites{

    private Map<String, HstSite> hstSites = Collections.synchronizedMap(new HashMap<String, HstSite>());
 
    public LazyLoadingHstSitesService(Node node) {
        super(node);
    }
   
    public void addHstSite(HstSite hstSite) {
        hstSites.put(hstSite.getName(), hstSite);
    }

    @Override 
    public Map<String, HstSite> getSites() {
        return this.hstSites;
    }
  

}
