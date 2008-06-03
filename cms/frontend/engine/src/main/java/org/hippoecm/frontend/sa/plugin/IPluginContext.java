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
package org.hippoecm.frontend.sa.plugin;

import java.util.List;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.sa.plugin.config.IClusterConfig;

public interface IPluginContext extends IDetachable {

    IPluginControl start(IClusterConfig cluster);

    <T extends IClusterable> T getService(String name, Class<T> clazz);

    <T extends IClusterable> List<T> getServices(String name, Class<T> clazz);

    <T extends IClusterable> IServiceReference<T> getReference(T service);

    /**
     * Registers a service with the given name.
     * 
     * @param name
     * @param service
     */
    void registerService(IClusterable service, String name);

    /**
     * Registers a service under the given name.
     * 
     * @param name
     * @param service
     */
    void unregisterService(IClusterable service, String name);

    /**
     * Registers a service with the given name.
     * 
     * @param name
     * @param service
     */
    void registerTracker(IServiceTracker listener, String name);

    /**
     * Unregisters a service with the given name.
     * 
     * @param name
     * @param service
     */
    void unregisterTracker(IServiceTracker listener, String name);

}
