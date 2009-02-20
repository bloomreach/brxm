package org.hippoecm.hst.core.container;

import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentConfigurationBean;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;

public class HstComponentFactoryImpl implements HstComponentFactory {
    
    protected HstComponentRegistry componentRegistry;
    
    public HstComponentFactoryImpl(HstComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }
    
    public HstComponent getComponentInstance(ServletConfig servletConfig, HstComponentConfiguration compConfig) throws HstComponentException {
        
        String componentId = compConfig.getId();
        HstComponent component = this.componentRegistry.getComponent(servletConfig, componentId);
        
        if (component == null) {
            
            String componentClassName = compConfig.getComponentClassName();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            try {
                
                Class compClass = loader.loadClass(componentClassName);
                component = (HstComponent) compClass.newInstance();
                
                final String componentContentBasePath = compConfig.getComponentContentBasePath();
                final String contextRelativePath = compConfig.getContextRelativePath();
                Map<String, Object> tempProperties = compConfig.getProperties();
                final Map<String, Object> properties = (Map<String, Object>) (tempProperties == null ? Collections.emptyMap() : Collections.unmodifiableMap(tempProperties));
                tempProperties = null;
                
                HstComponentConfigurationBean compConfigBean = new HstComponentConfigurationBean() {
                    public String getComponentContentBasePath() {
                        return componentContentBasePath;
                    }

                    public String getContextRelativePath() {
                        return contextRelativePath;
                    }

                    public Map<String, Object> getProperties() {
                        return properties;
                    }
                };
                
                component.init(servletConfig, compConfigBean);
                
                this.componentRegistry.registerComponent(servletConfig, componentId, component);
                
            } catch (ClassNotFoundException e) {
                throw new HstComponentException("Cannot find the class of " + componentId + ": " + componentClassName);
            } catch (InstantiationException e) {
                throw new HstComponentException("Cannot instantiate the class of " + componentId + ": " + componentClassName);
            } catch (IllegalAccessException e) {
                throw new HstComponentException("Illegal access to the class of " + componentId + ": " + componentClassName);
            }
        }
        
        return component;
        
    }

}
