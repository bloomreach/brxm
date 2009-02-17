package org.hippoecm.hst.core.container;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.container.HstComponentFactory;

public class HstComponentFactoryImpl implements HstComponentFactory {
    
    protected ServletConfig servletConfig;
    protected Map<String, HstComponent> componentMap = Collections.synchronizedMap(new HashMap<String, HstComponent>());
    
    public void setServletConfig(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
    }
    
    public HstComponent getComponentInstance(HstComponentConfiguration compConfig) throws HstComponentException {
        String compConfigId = compConfig.getId();
        HstComponent component = this.componentMap.get(compConfig.getId());
        
        if (component == null) {
            String componentClassName = compConfig.getComponentClassName();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            try {
                Class compClass = loader.loadClass(componentClassName);
                component = (HstComponent) compClass.newInstance();
                component.init(this.servletConfig);
                this.componentMap.put(compConfig.getId(), component);
            } catch (ClassNotFoundException e) {
                throw new HstComponentException("Cannot find the class of " + compConfigId + ": " + componentClassName);
            } catch (InstantiationException e) {
                throw new HstComponentException("Cannot instantiate the class of " + compConfigId + ": " + componentClassName);
            } catch (IllegalAccessException e) {
                throw new HstComponentException("Illegal access to the class of " + compConfigId + ": " + componentClassName);
            }
        }
        
        return component;
    }

}
