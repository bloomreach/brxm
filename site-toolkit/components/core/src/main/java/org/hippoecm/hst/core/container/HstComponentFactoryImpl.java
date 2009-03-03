/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.container;

import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.site.request.ComponentConfigurationImpl;

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
                
                Map<String, Object> tempProperties = compConfig.getProperties();
                final Map<String, Object> properties = (Map<String, Object>) (tempProperties == null ? Collections.emptyMap() : Collections.unmodifiableMap(tempProperties));
                tempProperties = null;
                
                ComponentConfiguration compConfigImpl = new ComponentConfigurationImpl(properties); 
                
                component.init(servletConfig, compConfigImpl);
                
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
