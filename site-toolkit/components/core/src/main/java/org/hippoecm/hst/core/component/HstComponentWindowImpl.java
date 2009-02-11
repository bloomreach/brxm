package org.hippoecm.hst.core.component;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public class HstComponentWindowImpl implements HstComponentWindow {
    
    protected String referenceName;
    protected String referenceNamespace;
    protected HstComponent hstComponent;
    protected String renderPath;
    protected Map<String, HstComponentWindow> childWindowMap;
    
    public HstComponentWindowImpl(HstComponentConfiguration compConfig) throws Exception {
        this(compConfig, null);
    }
    
    public HstComponentWindowImpl(HstComponentConfiguration compConfig, String namespacePrefix) throws Exception {
        
        this.referenceName = compConfig.getReferenceName();
        this.referenceNamespace = (namespacePrefix != null ? namespacePrefix + "." + this.referenceName : this.referenceName);
        
        String componentClassName = compConfig.getComponentClassName();
        Map<String, Object> properties = compConfig.getProperties();
        String referenceName = compConfig.getReferenceName();
        String renderPath = compConfig.getRenderPath();
        String contextRelativePath = compConfig.getContextRelativePath();
        String componentContentBasePath = compConfig.getComponentContentBasePath();
        Map<String, HstComponentConfiguration> children = compConfig.getChildren();

        Class componentClass = Class.forName(componentClassName);
        
        if (HstComponent.class.isAssignableFrom(componentClass)) {
            hstComponent = (HstComponent) componentClass.newInstance();
            hstComponent.init(properties);
        } else if (HstComponentFactory.class.isAssignableFrom(componentClass)) {
            HstComponentFactory factory = (HstComponentFactory) componentClass.newInstance();
            factory.init(properties);
            hstComponent = (HstComponent) factory.newInstance();
        } else {
            throw new IllegalArgumentException("Invalid component class configuration: " + componentClassName);
        }
        
        this.renderPath = compConfig.getRenderPath();
        
        this.childWindowMap = new HashMap<String, HstComponentWindow>();
        
        Map<String, HstComponentConfiguration> childCompConfigMap = compConfig.getChildren();
        
        if (childCompConfigMap != null) {
            for (Map.Entry<String, HstComponentConfiguration> entry : childCompConfigMap.entrySet()) {
                String childName = entry.getKey();
                HstComponentConfiguration childCompConfig = entry.getValue();
                this.childWindowMap.put(childName, new HstComponentWindowImpl(childCompConfig, this.referenceNamespace));
            }
        }
    }

    public HstComponent getComponent() {
        return this.hstComponent;
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
