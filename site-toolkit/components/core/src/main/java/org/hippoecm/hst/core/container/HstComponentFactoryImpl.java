/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstComponentMetadata;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.site.request.ComponentConfigurationImpl;

/**
 * HstComponentFactoryImpl
 */
public class HstComponentFactoryImpl implements HstComponentFactory {
    
    protected HstComponentRegistry componentRegistry;
    protected Class<?> defaultHstComponentClass = GenericHstComponent.class;
    protected String defaultHstComponentClassName = GenericHstComponent.class.getName();
    
    public HstComponentFactoryImpl(HstComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }
    
    public String getDefaultHstComponentClassName() {
        return defaultHstComponentClassName;
    }

    public Class<?> getDefaultHstComponentClass() {
        return defaultHstComponentClass;
    }

    public void setDefaultHstComponentClass(Class<?> defaultHstComponentClass) {
        this.defaultHstComponentClass = defaultHstComponentClass;
        defaultHstComponentClassName = defaultHstComponentClass.getName();
    }
    
    public HstComponent getComponentInstance(HstContainerConfig requestContainerConfig, HstComponentConfiguration compConfig, Mount mount) throws HstComponentException {
        String componentId = getComponentId(compConfig, mount);
        HstComponent component = this.componentRegistry.getComponent(requestContainerConfig, componentId);
        
        if (component == null) {
            boolean initialized = false;
            String componentClassName = StringUtils.trim(compConfig.getComponentClassName());
             try {

                Class<?> compClass = null;
                
                if (!StringUtils.isEmpty(componentClassName)) {
                    compClass = Class.forName(componentClassName);
                } else {
                    compClass = defaultHstComponentClass;
                    componentClassName = defaultHstComponentClassName;
                }
                
                component = (HstComponent) compClass.newInstance();
                ComponentConfiguration compConfigImpl = new ComponentConfigurationImpl(compConfig);
                component.init(requestContainerConfig.getServletContext(), compConfigImpl);
                
                initialized = true;
            } catch (ClassNotFoundException e) {
                throw new HstComponentException("Cannot find the class of " + compConfig.getCanonicalStoredLocation() + ": " + componentClassName);
            } catch (InstantiationException e) {
                throw new HstComponentException("Cannot instantiate the class of " + compConfig.getCanonicalStoredLocation() + ": " + componentClassName);
            } catch (IllegalAccessException e) {
                throw new HstComponentException("Illegal access to the class of " + compConfig.getCanonicalStoredLocation() + ": " + componentClassName);
            }
            
            if (initialized) {
                this.componentRegistry.registerComponent(requestContainerConfig, componentId, component);
            }
        }
        
        return component;
        
    }
    
    public HstComponentMetadata getComponentMetadata(HstContainerConfig requestContainerConfig, HstComponentConfiguration compConfig, Mount mount) throws HstComponentException {
        String componentId = getComponentId(compConfig, mount);
        return componentRegistry.getComponentMetadata(requestContainerConfig, componentId);
    }

    @SuppressWarnings("unchecked")
    public <T> T getObjectInstance(HstContainerConfig requestContainerConfig, String className) throws HstComponentException {
        try {
            Class<T> clazz = (Class<T>) Class.forName(className);
            return (T) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new HstComponentException("Cannot find the class of " + className);
        } catch (InstantiationException e) {
            throw new HstComponentException("Cannot instantiate the class of " + className);
        } catch (IllegalAccessException e) {
            throw new HstComponentException("Illegal access to the class of " + className);
        }
    }
    
    private String getComponentId(HstComponentConfiguration compConfig, Mount mount) {
        //String componentId = compConfig.getId() + compConfig.hashCode();
        StringBuilder componentIdBuilder = new StringBuilder();
        // account for the Mount info in the componentId since one and the same HstComponentConfiguration instance
        // can be shared by multiple mounts
        componentIdBuilder.append("mount:").append(mount.hashCode()).append('\uFFFF').append(mount.getIdentifier()).append('\uFFFF');
        componentIdBuilder.append("compId:").append(compConfig.getId()).append('\uFFFF').append(compConfig.hashCode());
        return componentIdBuilder.toString();
    }
}
