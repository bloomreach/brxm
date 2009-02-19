package org.hippoecm.hst.core.container;

import java.util.Map;

public interface HstContainerURL {
    
    String getType();
    
    String getActionWindow();
    
    String getResourceWindow();
    
    void setParameter(String name, String value);
    
    void setParameter(String name, String[] values);
    
    void setParameters(Map<String, String[]> parameters);
    
    String toString();
    
    Map<String, String[]> getParameterMap();
    
}
