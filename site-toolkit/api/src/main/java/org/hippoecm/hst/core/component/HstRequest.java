package org.hippoecm.hst.core.component;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.request.HstRequestContext;

public interface HstRequest extends HttpServletRequest {

    HstRequestContext getRequestContext();
    
    HstComponentWindow getComponentWindow();
    
    Map<String, Object> getParameterMap();
    
    Map<String, Object> getParameterMap(String namespace);
    
    Map<String, Object> getAttributeMap();
    
    Map<String, Object> getAttributeMap(String namespace);
    
}
