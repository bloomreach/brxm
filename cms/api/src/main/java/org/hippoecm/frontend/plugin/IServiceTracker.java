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
package org.hippoecm.frontend.plugin;

import org.apache.wicket.util.io.IClusterable;

/**
 * The service tracker interface makes service registry events accessible to plugins.
 * This is particularly useful for plugins that provide extension points.  Those extension points will
 * be populated by other plugins by their registration of additional services.  A service tracker allows
 * the extension point provider by responding in an appropriate manner.
 *
 * @param <S> the type of the service to track
 */
public interface IServiceTracker<S extends IClusterable> extends IClusterable {

    /**
     * A service is being registered.
     *
     * @param service the service that is registered
     * @param name the name that was used to register the service tracker
     */
    void addService(S service, String name);

    /**
     * A service is being unregistered.
     *
     * @param service the service that is unregistered
     * @param name the name that was used to register the service tracker
     */
    void removeService(S service, String name);
}
