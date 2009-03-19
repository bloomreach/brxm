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

/**
 * ComponentManager interface.
 * This is responsible for initializing, starting, stopping and closing container components.
 * 
 * @version $Id$
 */
public interface ComponentManager
{
    
    /**
     * Initializes the component manager and container components.
     */
    void initialize();
    
    /**
     * Starts the component manager to serve container components.
     */
    void start();
    
    /**
     * Returns the registered container component
     * 
     * @param <T> component type
     * @param name the name of the component
     * @return component
     */
    <T> T getComponent(String name);
    
    /**
     * Stop the component manager.
     */
    void stop();
    
    /**
     * Closes the component manager and all the components.
     */
    void close();
    
}
