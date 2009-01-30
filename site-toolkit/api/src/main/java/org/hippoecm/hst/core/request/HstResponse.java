package org.hippoecm.hst.core.request;

import java.io.Writer;
import java.util.Map;

public interface HstResponse
{
    
    Writer getWriter();
    
    void setRenderParameter(String name, String value);
    
    void setRenderParameter(String name, String [] values);
    
    void setRenderParameters(Map<String, String> params);
    
    void sendRedirect(String location);
    
}
