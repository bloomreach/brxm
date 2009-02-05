package org.hippoecm.hst.core.component;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public interface HstRequest extends HttpServletRequest {

    Map<String, Object> getParameterMap();
    
    Map<String, Object> getParameterMap(String namespace);
    
    Map<String, Object> getAttributeMap();
    
    Map<String, Object> getAttributeMap(String namespace);
    
}
