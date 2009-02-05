package org.hippoecm.hst.configuration.components;

import java.util.List;
import java.util.Map;

import org.hippoecm.hst.service.Service;

public interface HstComponentConfiguration extends Service{
 
    public String getName();
    
    public String getAlias();
    
    public String getRenderPath();
    
    public String getNamespace();
    
    public String getContextRelativePath();
    
    public String getComponentContentBasePath();;
    
    public String getComponentClassName();

    public HstComponentsConfiguration getHstComponents();
    
    public List<HstComponentConfiguration> getChildren();
    
    public Map<String, Object> getProperties();
 
}
