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

import java.util.EventObject;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * ComponentManager interface.
 * This is responsible for initializing, starting, stopping and closing container components.
 * 
 * @version $Id$
 */
public interface ComponentManager
{
    
    /**
     * Sets configuration resources for components assembly
     * @param configurationResources
     */
    void setConfigurationResources(String [] configurationResources);

    /**
     * Returns configuration resources for components assembly
     * @param configurationResource
     */
    String [] getConfigurationResources();

    /**
     * Set the ServletConfig that this object runs in.
     * @param servletConfig
     */
    void setServletConfig(ServletConfig servletConfig);

    /**
     * @return the ServletConfig that this object runs in.
     */
    ServletConfig getServletConfig();

    /**
     * Set the ServletContext that this object runs in.
     * @param servletContext
     */
    void setServletContext(ServletContext servletContext);

    /**
     * @return the ServletContext that this object runs in
     */
    ServletContext getServletContext();

    /**
     * Initializes the component manager and container components.
     */
    void initialize();
    
    /**
     * Starts the component manager to serve container components.
     */
    void start();
    
    /**
     * Returns the registered container component by name.
     * Returns null if a component is not found by the specified name.
     * 
     * @param <T> component type
     * @param name the name of the component
     * @return component
     */
    <T> T getComponent(String name);

    /**
     * Returns the registered container component by the specified type if any.
     * Implementation may also look up a component by translating the specified type into a conventional FQCN string of the type
     * if a component is not found by the specified type.
     * Returns null if no component is found.
     * If there is not exactly single matching component found by the specified type, then it throws a {@link ComponentsException}.
     * 
     * @param requiredType type the bean must match; can be an interface or superclass.
     * @return component matching the required type or null if not found by the specified type
     * @throws ComponentsException
     */
    <T> T getComponent(Class<T> requiredType) throws ComponentsException;

    /**
     * Returns the registered container components that match the given object type (including subclasses).
     * Returns empty map if a component is not found by the specified required type.
     * 
     * @param <T> component type
     * @param requiredType the required type of the component
     * @return component map
     */
    <T> Map<String, T> getComponentsOfType(Class<T> requiredType);

    /**
     * Returns the registered component from a child context.
     * If <CODE>addonModuleNames</CODE> consists of multiple items, then 
     * each <CODE>addonModuleNames</CODE> item is regarded as child addon module name 
     * in the descendant hierarchy, as ordered.
     * Returns null if a component is not found by the specified name.
     * 
     * @param <T>
     * @param name
     * @param addonModuleNames
     * @return
     * @throws ModuleNotFoundException thrown when module is not found by the addonModuleNames
     */
    <T> T getComponent(String name, String ... addonModuleNames);

    /**
     * Returns the registered component from a child context by the specified type.
     * Implementation may also look up a component by translating the specified type into a conventional FQCN string of the type
     * if a component is not found by the specified type.
     * Returns null if no component is found.
     * If there is not exactly single matching component found by the specified type, then it throws a {@link ComponentsException}.
     * If <CODE>addonModuleNames</CODE> consists of multiple items, then 
     * each <CODE>addonModuleNames</CODE> item is regarded as child addon module name 
     * in the descendant hierarchy, as ordered.
     * @param requiredType type the bean must match; can be an interface or superclass.
     * @param addonModuleNames
     * @return component matching the required type or null if not found by the specified type
     * @throws ComponentsException
     */
    <T> T getComponent(Class<T> requiredType, String ... addonModuleNames) throws ComponentsException;

    /**
     * Returns the registered container component that match the given object type (including subclasses) from a child context.
     * Returns empty map if a component is not found from a child context
     * by the specified required type.
     * If <CODE>addonModuleNames</CODE> consists of multiple items, then 
     * each <CODE>addonModuleNames</CODE> item is regarded as child addon module name 
     * in the descendant hierarchy, as ordered.
     * Returns empty map if a component is not found by the specified type.
     * 
     * @param <T>
     * @param requiredType
     * @param addonModuleNames
     * @return component map
     * @throws ModuleNotFoundException thrown when module is not found by the addonModuleNames
     */
    <T> Map<String, T> getComponentsOfType(Class<T> requiredType, String ... addonModuleNames);

    /**
     * Publish the given event to all components which wants to listen to.
     * @param event the event to publish (may be an application-specific or built-in HST-2 event)
     */
    void publishEvent(EventObject event);

    /**
     * Registers event subscriber object to receive events.
     * @param subscriber
     */
    void registerEventSubscriber(Object subscriber);

    /**
     * Unregisters event subscriber object.
     * @param subscriber
     */
    void unregisterEventSubscriber(Object subscriber);

    /**
     * Stop the component manager.
     */
    void stop();
    
    /**
     * Closes the component manager and all the components.
     */
    void close();
    
    /**
     * Returns the container configuration
     * @return
     */
    ContainerConfiguration getContainerConfiguration();
    
}
