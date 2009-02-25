package org.hippoecm.hst.site.request;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

public class ResolvedSiteMapItemImpl implements ResolvedSiteMapItem{
    
    private HstSiteMap hstSiteMap;
    private Map<String, Object> resolvedProperties;
    private String[] params;
    
    public ResolvedSiteMapItemImpl(HstSiteMapItem hstSiteMapItem , String[] params) {
       this.hstSiteMap = hstSiteMapItem.getHstSiteMap();
       this.resolvedProperties = new HashMap<String, Object>();
       this.params = params;
       
       /*
        * We take the properties form the hstSiteMapItem getProperties and replace params (like $1) with the params[] array 
        */
       
       this.resolvedProperties = hstSiteMapItem.getProperties();
       
       
       
    }
    
    public Object getProperty(String name) {
        return resolvedProperties.get(name);
    }

    public HstSiteMap getHstSiteMap() {
        return this.hstSiteMap;
    }

    
  
}
