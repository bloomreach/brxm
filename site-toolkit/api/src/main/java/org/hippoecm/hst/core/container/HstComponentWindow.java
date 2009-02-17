package org.hippoecm.hst.core.container;

import java.util.Map;

import org.hippoecm.hst.core.component.HstComponent;

public interface HstComponentWindow {

    String getReferenceName();
    
    String getReferenceNamespace();
    
    HstComponent getComponent();
    
    String getRenderPath(); 
    
    HstComponentWindow getParentWindow();
    
    Map<String, HstComponentWindow> getChildWindowMap();
    
}
