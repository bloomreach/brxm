package org.hippoecm.hst.core.container;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.container.HstComponentWindow;

public class HstComponentWindowImpl implements HstComponentWindow {
    
    protected String referenceName;
    protected String referenceNamespace;
    protected HstComponent component;
    protected String renderPath;
    protected HstComponentWindow parentWindow;
    protected Map<String, HstComponentWindow> childWindowMap;
    
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

    public String getReferenceName() {
        return this.referenceName;
    }

    public String getReferenceNamespace() {
        return this.referenceNamespace;
    }
    
    protected void addChildWindow(String name, HstComponentWindow child) {
        if (this.childWindowMap == null) {
            this.childWindowMap = new HashMap<String, HstComponentWindow>();
        }
        
        this.childWindowMap.put(name, child);
    }

}
