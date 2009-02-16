package org.hippoecm.hst.core.component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public class HstComponentFactoryImpl implements HstComponentFactory {
    
    protected Map<String, HstComponentContext> componentContextMap = Collections.synchronizedMap(new HashMap<String, HstComponentContext>());

    public ClassLoader getComponentContextClassLoader(String contextName) {
        ClassLoader loader = null;
        
        HstComponentContext context = this.componentContextMap.get(contextName);
        
        if (context != null) {
            loader = context.getClassLoader();
        }
        
        return loader;
    }

    public HstComponent getComponentInstance(HstComponentConfiguration compConfig) throws HstComponentException {
        String contextName = compConfig.getComponentContextName();
        
        if (contextName == null) {
            contextName = HstComponentContext.LOCAL_COMPONENT_CONTEXT_NAME;
        }
        
        HstComponentContext context = this.componentContextMap.get(contextName);
        
        if (context == null) {
            throw new HstComponentException("The HST component context is not registered: " + contextName);
        }
        
        HstComponent component = context.getComponent(compConfig.getId());
        
        if (component == null) {
            ClassLoader loader = context.getClassLoader();
            
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            
            ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
            
            try {
                Class compClass = loader.loadClass(compConfig.getComponentClassName());
                component = (HstComponent) compClass.newInstance();
                component.init(context.getServletConfig());
            } catch (ClassNotFoundException e) {
                throw new HstComponentException(e);
            } catch (InstantiationException e) {
                throw new HstComponentException(e);
            } catch (IllegalAccessException e) {
                throw new HstComponentException(e);
            } finally {
                Thread.currentThread().setContextClassLoader(oldLoader);
            }
        }
        
        return component;
    }

    public void registerComponentContext(String contextName, ServletConfig servletConfig, ClassLoader classLoader) {
        HstComponentContext context = new HstComponentContextImpl(contextName, servletConfig, classLoader);
        this.componentContextMap.put(contextName, context);
    }

    public void unregisterComponentContext(String contextName) {
        this.componentContextMap.remove(contextName);
    }

}
