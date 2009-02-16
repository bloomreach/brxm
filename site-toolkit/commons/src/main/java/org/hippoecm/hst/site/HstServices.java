package org.hippoecm.hst.site;

import org.hippoecm.hst.core.component.HstComponentFactory;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.HstComponentInvoker;
import org.hippoecm.hst.core.container.HstComponentInvokerProvider;
import org.hippoecm.hst.core.container.HstRequestProcessor;

public class HstServices {
    
    private static boolean available;
    private static ComponentManager componentManager;

    private HstServices() {
    }
    
    public static void setComponentManager(ComponentManager compManager) {
        HstServices.componentManager = compManager;
        HstServices.available = true;
    }
    
    public static ComponentManager getComponentManager() {
        return HstServices.componentManager;
    }
    
    public static boolean isAvailable() {
        return HstServices.available;
    }
    
    public static HstComponentFactory getComponentFactory() {
        return getComponentManager().<HstComponentFactory>getComponent(HstComponentFactory.class.getName());
    }
    
    public static HstRequestProcessor getRequestProcessor() {
        return getComponentManager().<HstRequestProcessor>getComponent(HstRequestProcessor.class.getName());
    }
    
    public static HstComponentInvokerProvider getComponentInvokerProvider() {
        return getComponentManager().<HstComponentInvokerProvider>getComponent(HstComponentInvokerProvider.class.getName());
    }
}
