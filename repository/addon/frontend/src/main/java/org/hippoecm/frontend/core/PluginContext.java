/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.core;

import java.io.Serializable;
import java.util.Map;

public interface PluginContext {

    Plugin start(PluginConfig config);

    Map<String, Object> getProperties();

    /**
     * Registers a service with the given name.
     * 
     * @param name
     * @param service
     */
    void registerService(Serializable service, String name);

    /**
     * Registers a service under the given name.
     * 
     * @param name
     * @param service
     */
    void unregisterService(Serializable service, String name);

    /**
     * Registers a service with the given name.
     * 
     * @param name
     * @param service
     */
    void registerListener(ServiceListener listener, String name);

    /**
     * Unregisters a service with the given name.
     * 
     * @param name
     * @param service
     */
    void unregisterListener(ServiceListener listener, String name);

}
