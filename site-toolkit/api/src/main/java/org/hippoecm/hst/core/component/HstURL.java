package org.hippoecm.hst.core.component;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public interface HstURL {
    
    /**
     * URL type which can one of these: 'action', 'render', or 'resource'.
     * 
     * @param type
     */
    void setType(String type);
    
    String getType();
    
    void setParameter(String name, String value);
    
    void setParameter(String name, String[] values);
    
    void setParameters(java.util.Map<String, String[]> parameters);
    
    String toString();
    
    Map<String, String[]> getParameterMap();
    
    void write(Writer out) throws IOException;
    
    void write(Writer out, boolean escapeXML) throws IOException;
    
    /**
     * Allows setting a resource ID that can be retrieved when serving the resource
     * through HstRequest.getResourceID() method in a HstComponent instance. 
     * 
     * @param resourceID
     */
    void setResourceID(String resourceID);
    
}
