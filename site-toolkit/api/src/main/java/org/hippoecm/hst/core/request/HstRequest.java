package org.hippoecm.hst.core.request;

import java.util.Map;

public interface HstRequest
{
    
    void setSessionAttribute(String name, Object value);
    
    Object getSessionAttribute(String name);
    
    void setGlobalSessionAttribute(String name, Object value);
    
    Object getGlobalSessionAttribute(String name);
    
    void setRequestAttribute(String name, Object value);
    
    Object getRequestAttribute(String name);
    
    Map<String, String> getParameterMap();
    
}
