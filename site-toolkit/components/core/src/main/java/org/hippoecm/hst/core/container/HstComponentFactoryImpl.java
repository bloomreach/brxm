/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
public class HstComponentFactoryImpl implements HstComponentFactory, ComponentManagerAware {

    protected Class<?> defaultHstComponentClass = GenericHstComponent.class;
    protected String defaultHstComponentClassName = GenericHstComponent.class.getName();

    private ComponentManager componentManager;

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
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

        final  HstComponentRegistry componentRegistry = mount.getVirtualHost().getVirtualHosts().getComponentRegistry();
        HstComponent component = componentRegistry.getComponent(requestContainerConfig, componentId);

        if (component == null) {
            String componentClassName = StringUtils.trim(compConfig.getComponentClassName());
            try {
                Class<?> compClass;

                if (StringUtils.isNotEmpty(componentClassName)) {
                    // first try to retrieve component bean from the componentManager by classname.
                    component = componentManager.getComponent(componentClassName);

                    // if component not found from componentManager, instantiate it from classname.
                    if (component == null) {
                        compClass = Class.forName(componentClassName);
                        component = (HstComponent) compClass.newInstance();
                    }
                } else {
                    compClass = defaultHstComponentClass;
                    componentClassName = defaultHstComponentClassName;
                    component = (HstComponent) compClass.newInstance();
                }

                ComponentConfiguration compConfigImpl = new ComponentConfigurationImpl(compConfig);
                component.init(requestContainerConfig.getServletContext(), compConfigImpl);

            } catch (ClassNotFoundException e) {
                HstComponentException exc = new HstComponentException("Cannot find the class of " + compConfig.getCanonicalStoredLocation() + ": " + componentClassName);
                componentRegistry.registerComponent(requestContainerConfig, componentId, new FailedComponent(exc));
                throw  exc;
            } catch (InstantiationException e) {
                HstComponentException exc = new HstComponentException("Cannot instantiate the class of " + compConfig.getCanonicalStoredLocation() + ": " + componentClassName);
                componentRegistry.registerComponent(requestContainerConfig, componentId, new FailedComponent(exc));
                throw exc;
            } catch (IllegalAccessException e) {
                HstComponentException exc = new HstComponentException("Illegal access to the class of " + compConfig.getCanonicalStoredLocation() + ": " + componentClassName);
                componentRegistry.registerComponent(requestContainerConfig, componentId, new FailedComponent(exc));
                throw exc;
            }
            componentRegistry.registerComponent(requestContainerConfig, componentId, component);
        }

        if (component instanceof FailedComponent) {
            throw ((FailedComponent)component).exc;
        }
        return component;
        
    }
    
    public HstComponentMetadata getComponentMetadata(HstContainerConfig requestContainerConfig, HstComponentConfiguration compConfig, Mount mount) throws HstComponentException {
        String componentId = getComponentId(compConfig, mount);
        final  HstComponentRegistry componentRegistry = mount.getVirtualHost().getVirtualHosts().getComponentRegistry();
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

    private static final class FailedComponent extends GenericHstComponent {
        private HstComponentException exc;
        public FailedComponent(final HstComponentException exc) {
            this.exc = exc;
        }
    }

}
