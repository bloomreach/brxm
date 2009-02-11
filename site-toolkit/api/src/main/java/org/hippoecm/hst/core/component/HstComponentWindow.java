package org.hippoecm.hst.core.component;

import java.util.Map;

public interface HstComponentWindow {

    String getReferenceName();
    
    String getReferenceNamespace();
    
    HstComponent getComponent();
    
    String getRenderPath(); 
    
    Map<String, HstComponentWindow> getChildWindowMap();
    
}
