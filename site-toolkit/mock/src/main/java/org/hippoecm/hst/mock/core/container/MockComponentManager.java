/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.mock.core.container;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConfiguration;

public class MockComponentManager implements ComponentManager {

    private Map<String, Object> components = new HashMap<String, Object>();

    private ServletConfig servletConfig;

    private ServletContext servletContext;

    @Override
    public void setConfigurationResources(final String[] configurationResources) {
        // do nothing
    }

    @Override
    public String[] getConfigurationResources() {
        return new String[0];
    }

    @Override
    public void setServletConfig(final ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
        if (servletConfig != null) {
          this.servletContext = servletConfig.getServletContext();
        }
    }

    @Override
    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    @Override
    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void initialize() {
        // do nothing
    }

    @Override
    public void start() {
        // do nothing
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getComponent(final String name) {
        return (T) components.get(name);
    }

    @Override
    public <T> Map<String, T> getComponentsOfType(final Class<T> requiredType) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getComponent(final String name, final String... addonModuleNames) {
        return (T) components.get(name);
    }

    @Override
    public <T> Map<String, T> getComponentsOfType(final Class<T> requiredType, final String... addonModuleNames) {
        return null;
    }

    @Override
    public void publishEvent(final EventObject event) {
        // do nothing
    }

    @Override
    public void registerEventSubscriber(final Object subscriber) {
        // do nothing
    }

    @Override
    public void unregisterEventSubscriber(final Object subscriber) {
        // do nothing
    }

    @Override
    public void stop() {
        // do nothing
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public ContainerConfiguration getContainerConfiguration() {
        return null;
    }

    public <T> void addComponent(String name, T component) {
        components.put(name, component);
    }
}
