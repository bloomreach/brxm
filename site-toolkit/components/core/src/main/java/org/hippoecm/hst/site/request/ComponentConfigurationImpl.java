package org.hippoecm.hst.site.request;

import java.util.Map;

import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

public class ComponentConfigurationImpl implements ComponentConfiguration{

    public Map<String, Object> unmodifiablePropertiesMap;
    
    public ComponentConfigurationImpl(Map<String, Object> properties) {
        this.unmodifiablePropertiesMap = properties;
    }

    public Object getProperty(String name, ResolvedSiteMapItem hstMatchedSiteMapItem) {
        // TODO IF an expression exists, like ${year}, try to fetch hstMatchedSiteMapItem.getProperty("year") as this should 
        // have been substituted during creation of the MatchingSiteMapItem
        
        return unmodifiablePropertiesMap.get(name);
    }

}
