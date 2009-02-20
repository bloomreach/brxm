package org.hippoecm.hst.core.container;

import java.util.Map;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstComponentWindowFactoryImpl implements HstComponentWindowFactory {
    
    protected String namespaceSeparator = ContainerConstants.DEFAULT_COMPONENT_WINDOW_NAMESPACE_SEPARATOR;
    
    public void setNamespaceSeparator(String namespaceSeparator) {
        this.namespaceSeparator = namespaceSeparator;
    }

    public HstComponentWindow create(ServletConfig servletConfig, HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory) throws HstComponentException {
        return create(servletConfig, requestContext, compConfig, compFactory, null);
    }
    
    public HstComponentWindow create(ServletConfig servletConfig, HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory, HstComponentWindow parentWindow) throws HstComponentException {
        
        String referenceName = compConfig.getReferenceName();
        StringBuilder referenceNamespaceBuilder = new StringBuilder();

        if (parentWindow == null) {
            String contextNamespace = requestContext.getContextNamespace();
            
            if (contextNamespace != null) {
                referenceNamespaceBuilder.append(contextNamespace);
            }            
        } else {
            String parentReferenceNamespace = parentWindow.getReferenceNamespace();
            
            if ("".equals(parentReferenceNamespace)) {
                referenceNamespaceBuilder.append(referenceName);
            } else {
                referenceNamespaceBuilder.append(parentReferenceNamespace).append(this.namespaceSeparator).append(referenceName);
            }
        }

        String referenceNamespace = referenceNamespaceBuilder.toString();
        
        String renderPath = compConfig.getRenderPath();
        
        if (renderPath != null && !renderPath.startsWith("/")) {
            renderPath = new StringBuilder(renderPath.length() + 1).append('/').append(renderPath).toString();
        }
        
        HstComponent component = compFactory.getComponentInstance(servletConfig, compConfig);
        
        HstComponentWindowImpl window = 
            new HstComponentWindowImpl(
                referenceName, 
                referenceNamespace, 
                component, 
                renderPath, 
                parentWindow);
        
        Map<String, HstComponentConfiguration> childCompConfigMap = compConfig.getChildren();
        
        if (childCompConfigMap != null && !childCompConfigMap.isEmpty()) {
            for (Map.Entry<String, HstComponentConfiguration> entry : childCompConfigMap.entrySet()) {
                HstComponentConfiguration childCompConfig = entry.getValue();
                HstComponentWindow childCompWindow = create(servletConfig, requestContext, childCompConfig, compFactory, window);
                window.addChildWindow(childCompWindow);
            }
        }
        
        return window;
    }

}
