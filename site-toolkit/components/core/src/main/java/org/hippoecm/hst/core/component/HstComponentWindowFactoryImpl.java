package org.hippoecm.hst.core.component;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public class HstComponentWindowFactoryImpl implements HstComponentWindowFactory {

    public HstComponentWindow create(HstComponentConfiguration compConfig, HstComponentFactory compFactory) throws HstComponentException {
        return create(compConfig, compFactory, null);
    }
    
    public HstComponentWindow create(HstComponentConfiguration compConfig, HstComponentFactory compFactory, String namespacePrefix) throws HstComponentException {
        
        String referenceName = compConfig.getReferenceName();
        String referenceNamespace = null;
        
        if (namespacePrefix == null) {
            referenceNamespace = "";
        } else {
            referenceNamespace = ("".equals(namespacePrefix) ? referenceName : namespacePrefix + "_" + referenceName);
        }

        String contextName = compConfig.getComponentContextName();
        
        if (contextName == null) {
            contextName = HstComponentContext.LOCAL_COMPONENT_CONTEXT_NAME;
        }
        
        String renderPath = compConfig.getRenderPath();
        HstComponent component = compFactory.getComponentInstance(compConfig);
        
        Map<String, HstComponentWindow> childWindowMap = null;
        
        Map<String, HstComponentConfiguration> childCompConfigMap = compConfig.getChildren();
        
        if (childCompConfigMap != null && !childCompConfigMap.isEmpty()) {
            childWindowMap = new HashMap<String, HstComponentWindow>();
            
            for (Map.Entry<String, HstComponentConfiguration> entry : childCompConfigMap.entrySet()) {
                String childName = entry.getKey();
                HstComponentConfiguration childCompConfig = entry.getValue();
                HstComponentWindow childCompWindow = create(childCompConfig, compFactory, referenceNamespace);
                childWindowMap.put(childName, childCompWindow);
            }
        }
        
        HstComponentWindowImpl window = 
            new HstComponentWindowImpl(
                contextName, 
                referenceName, 
                referenceNamespace, 
                component, 
                renderPath, 
                childWindowMap);
        
        return window;
    }

}
