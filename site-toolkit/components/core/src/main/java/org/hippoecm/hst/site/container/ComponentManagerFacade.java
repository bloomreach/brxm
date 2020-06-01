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
package org.hippoecm.hst.site.container;

import java.util.EventObject;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.container.ComponentsException;
import org.hippoecm.hst.core.container.ContainerConfiguration;

/**
 * ComponentManagerFacade
 * 
 * @version $Id$
 */
public class ComponentManagerFacade implements ComponentManagerAware, ComponentManager {
    
    protected ComponentManager componentManager;

    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public ComponentManager getComponentManager() {
        return this.componentManager;
    }

    /**
     * @deprecated deprecated since since 3.2.0
     * @see ComponentManager#setServletConfig(javax.servlet.ServletConfig)
     */
    @Deprecated
    public void setServletConfig(ServletConfig servletConfig) {
        this.componentManager.setServletConfig(servletConfig);
    }

    /**
     * @deprecated deprecated since since 3.2.0
     * @see ComponentManager#getServletConfig()
     */
    @Deprecated
    public ServletConfig getServletConfig() {
        return this.componentManager.getServletConfig();
    }

    public void setServletContext(ServletContext servletContext) {
        this.componentManager.setServletContext(servletContext);
    }

    public ServletContext getServletContext() {
        return this.componentManager.getServletContext();
    }

    public void close() {
        this.componentManager.close();
    }

    public <T> T getComponent(String name) {
        return this.componentManager.<T>getComponent(name);
    }

    public <T> T getComponent(Class<T> requiredType) throws ComponentsException {
        return this.componentManager.<T>getComponent(requiredType);
    }

    public <T> T getComponent(String name, String ... contextNames) {
        return this.componentManager.<T>getComponent(name, contextNames);
    }

    public <T> T getComponent(Class<T> requiredType, String... contextNames) throws ComponentsException {
        return this.componentManager.<T>getComponent(requiredType, contextNames);
    }

    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType) {
        return this.componentManager.getComponentsOfType(requiredType);
    }

    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType, String ... contextNames) {
        return this.componentManager.getComponentsOfType(requiredType, contextNames);
    }

    public void publishEvent(EventObject event) {
        this.componentManager.publishEvent(event);
    }

    public void registerEventSubscriber(Object subscriber) {
        this.componentManager.registerEventSubscriber(subscriber);
    }

    public void unregisterEventSubscriber(Object subscriber) {
        this.componentManager.unregisterEventSubscriber(subscriber);
    }

    public ContainerConfiguration getContainerConfiguration() {
        return this.componentManager.getContainerConfiguration();
    }

    public void initialize() {
        this.componentManager.initialize();
    }

    public void start() {
        this.componentManager.start();
    }

    public void stop() {
        this.componentManager.stop();
    }

    public void setConfigurationResources(String[] configurationResources) {
        this.componentManager.setConfigurationResources(configurationResources);
    }
    
    public String[] getConfigurationResources() {
        return this.componentManager.getConfigurationResources();
    }

}
