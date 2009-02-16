package org.hippoecm.hst.core.component;

import java.util.Map;

public class HstComponentWindowImpl implements HstComponentWindow {
    
    protected String contextName;
    protected String referenceName;
    protected String referenceNamespace;
    protected HstComponent component;
    protected String renderPath;
    protected Map<String, HstComponentWindow> childWindowMap;
    
    public HstComponentWindowImpl(String contextName, String referenceName, String referenceNamespace, HstComponent component, String renderPath, Map<String, HstComponentWindow> childWindowMap) {
        this.contextName = contextName;
        this.referenceName = referenceName;
        this.referenceNamespace = referenceNamespace;
        this.component = component;
        this.renderPath = renderPath;
        this.childWindowMap = childWindowMap;
    }
    
    public String getContextName() {
        return this.contextName;
    }
    
    public HstComponent getComponent() {
        return this.component;
    }
    
    public String getRenderPath() {
        return this.renderPath;
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

}
