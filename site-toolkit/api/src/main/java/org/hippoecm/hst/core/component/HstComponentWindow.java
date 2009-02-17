package org.hippoecm.hst.core.component;

import java.util.Map;

public interface HstComponentWindow {

    String getContextName();
    
    String getReferenceName();
    
    String getReferenceNamespace();
    
    HstComponent getComponent();
    
    String getRenderPath(); 
    
    HstComponentWindow getParentWindow();
    
    Map<String, HstComponentWindow> getChildWindowMap();
    
}
