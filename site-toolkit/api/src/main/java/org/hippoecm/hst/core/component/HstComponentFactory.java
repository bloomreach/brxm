package org.hippoecm.hst.core.component;

import java.util.Map;

public interface HstComponentFactory {
    
    void init(Map<String, Object> properties);
    
    HstComponent newInstance();
    
}
