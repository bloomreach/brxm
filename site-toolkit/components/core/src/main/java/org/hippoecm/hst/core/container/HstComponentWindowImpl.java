package org.hippoecm.hst.core.container;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.container.ContainerConstants;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstResponseState;
import org.springframework.util.StringUtils;

public class HstComponentWindowImpl implements HstComponentWindow {
    
    protected String referenceName;
    protected String referenceNamespace;
    protected HstComponent component;
    protected String renderPath;
    protected HstComponentWindow parentWindow;
    protected Map<String, HstComponentWindow> childWindowMap;
    
    protected HstResponseState responseState;
    
    public HstComponentWindowImpl(String referenceName, String referenceNamespace, HstComponent component, String renderPath, HstComponentWindow parentWindow) {
        this.referenceName = referenceName;
        this.referenceNamespace = referenceNamespace;
        this.component = component;
        this.renderPath = renderPath;
        this.parentWindow = parentWindow;
    }
    
    public HstComponent getComponent() {
        return this.component;
    }
    
    public String getRenderPath() {
        return this.renderPath;
    }
    
    public HstComponentWindow getParentWindow() {
        return this.parentWindow;
    }

    public Map<String, HstComponentWindow> getChildWindowMap() {
        return this.childWindowMap;
    }
    
    public HstComponentWindow getChildWindow(String referenceName) {
        HstComponentWindow childWindow = null;
        
        if (this.childWindowMap != null) {
            childWindow = this.childWindowMap.get(referenceName);
        }
        
        return childWindow;
    }

    public String getReferenceName() {
        return this.referenceName;
    }

    public String getReferenceNamespace() {
        return this.referenceNamespace;
    }
    
    public void flushContent() throws IOException {
        if (this.responseState != null) {
            if (this.parentWindow != null) {
                ((HstComponentWindowImpl) this.parentWindow).responseState.flushBuffer();
            }
            
            this.responseState.flush();
        }
    }
    
    protected void addChildWindow(HstComponentWindow child) {
        if (this.childWindowMap == null) {
            this.childWindowMap = new HashMap<String, HstComponentWindow>();
        }
        
        this.childWindowMap.put(child.getReferenceName(), child);
    }

    protected void setResponseState(HstResponseState responseState) {
        this.responseState = responseState;
    }
    
}
