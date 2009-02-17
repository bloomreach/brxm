package org.hippoecm.hst.core.component;

import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public class HstComponentWindowFactoryImpl implements HstComponentWindowFactory {

    public HstComponentWindow create(HstComponentConfiguration compConfig, HstComponentFactory compFactory) throws HstComponentException {
        return create(compConfig, compFactory, null);
    }
    
    public HstComponentWindow create(HstComponentConfiguration compConfig, HstComponentFactory compFactory, HstComponentWindow parentWindow) throws HstComponentException {
        
        String referenceName = compConfig.getReferenceName();
        String referenceNamespace = null;
        
        if (parentWindow == null) {
            referenceNamespace = "";
        } else {
            referenceNamespace = parentWindow.getReferenceNamespace() + "_" + referenceName;
        }

        String contextName = compConfig.getComponentContextName();
        
        if (contextName == null) {
            contextName = HstComponentContext.LOCAL_COMPONENT_CONTEXT_NAME;
        }
        
        String renderPath = compConfig.getRenderPath();
        
        if (renderPath != null && !renderPath.startsWith("/")) {
            renderPath = new StringBuilder(renderPath.length() + 1).append('/').append(renderPath).toString();
        }
        
        HstComponent component = compFactory.getComponentInstance(compConfig);
        
        HstComponentWindowImpl window = 
            new HstComponentWindowImpl(
                contextName, 
                referenceName, 
                referenceNamespace, 
                component, 
                renderPath, 
                parentWindow);
        
        Map<String, HstComponentConfiguration> childCompConfigMap = compConfig.getChildren();
        
        if (childCompConfigMap != null && !childCompConfigMap.isEmpty()) {
            for (Map.Entry<String, HstComponentConfiguration> entry : childCompConfigMap.entrySet()) {
                String childName = entry.getKey();
                HstComponentConfiguration childCompConfig = entry.getValue();
                HstComponentWindow childCompWindow = create(childCompConfig, compFactory, window);
                window.addChildWindow(childName, childCompWindow);
            }
        }
        
        return window;
    }

}
