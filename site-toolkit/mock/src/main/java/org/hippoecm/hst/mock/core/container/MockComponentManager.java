/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentsException;
import org.hippoecm.hst.core.container.ContainerConfiguration;

public class MockComponentManager implements ComponentManager {

    private Map<String, Object> components = new HashMap<String, Object>();

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

    @SuppressWarnings("unchecked")
    public <T> T getComponent(final Class<T> requiredType) throws ComponentsException {
        List<Object> beans = new ArrayList<Object>();

        for (Map.Entry<String, Object> entry : components.entrySet()) {
            Object component = entry.getValue();

            if (component != null && requiredType.isAssignableFrom(component.getClass())) {
                beans.add(component);
            }
        }

        if (beans.isEmpty()) {
            return null;
        }

        if (beans.size() != 1) {
            throw new ComponentsException("Multiple components found by the specified type, " + requiredType);
        }

        return (T) beans.get(0);
    }

    @Override
    public <T> Map<String, T> getComponentsOfType(final Class<T> requiredType) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getComponent(final String name, final String... addonModuleNames) {
        return getComponent(name);
    }

    public <T> T getComponent(final Class<T> requiredType, final String... addonModuleNames) {
        return getComponent(requiredType);
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
