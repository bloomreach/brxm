package org.hippoecm.hst.core.component;

import java.util.Map;

public interface HstURLProvider {

    void setType(String type);
    
    void setParameters(Map<String, String[]> parameters);
    
    void clearParameters();
    
    String toString();
    
}
