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

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.site.request.ComponentConfigurationImpl;

/**
 * HstComponentFactoryImpl
 * 
 * @version $Id$
 */
public class HstComponentFactoryImpl implements HstComponentFactory {
    
    protected HstComponentRegistry componentRegistry;
    
    public HstComponentFactoryImpl(HstComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }
    
    public HstComponent getComponentInstance(HstContainerConfig requestContainerConfig, HstComponentConfiguration compConfig) throws HstComponentException {
        
        String componentId = compConfig.getId();
        HstComponent component = this.componentRegistry.getComponent(requestContainerConfig, componentId);
        
        if (component == null) {
            boolean initialized = false;
            String componentClassName = compConfig.getComponentClassName();
            
            ClassLoader containerClassloader = requestContainerConfig.getContextClassLoader();
            ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();

            try {
                if (containerClassloader != currentClassloader) {
                    Thread.currentThread().setContextClassLoader(containerClassloader);
                }
                
                Class compClass = containerClassloader.loadClass(componentClassName);
                component = (HstComponent) compClass.newInstance();
               
                ComponentConfiguration compConfigImpl = new ComponentConfigurationImpl(compConfig); 
                
                component.init(requestContainerConfig.getServletContext(), compConfigImpl);
                
                initialized = true;
            } catch (ClassNotFoundException e) {
                throw new HstComponentException("Cannot find the class of " + componentId + ": " + componentClassName);
            } catch (InstantiationException e) {
                throw new HstComponentException("Cannot instantiate the class of " + componentId + ": " + componentClassName);
            } catch (IllegalAccessException e) {
                throw new HstComponentException("Illegal access to the class of " + componentId + ": " + componentClassName);
            } finally {
                if (containerClassloader != currentClassloader) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }
            
            if (initialized) {
                this.componentRegistry.registerComponent(requestContainerConfig, componentId, component);
            }
        }
        
        return component;
        
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getObjectInstance(HstContainerConfig requestContainerConfig, String className) throws HstComponentException {
        T object = null;
        
        ClassLoader containerClassloader = requestContainerConfig.getContextClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();

        try {
            if (containerClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(containerClassloader);
            }
            
            Class<T> clazz = (Class<T>) containerClassloader.loadClass(className);
            object = (T) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new HstComponentException("Cannot find the class of " + className);
        } catch (InstantiationException e) {
            throw new HstComponentException("Cannot instantiate the class of " + className);
        } catch (IllegalAccessException e) {
            throw new HstComponentException("Illegal access to the class of " + className);
        } finally {
            if (containerClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }                
        }
        
        return object;
    }
    
}
