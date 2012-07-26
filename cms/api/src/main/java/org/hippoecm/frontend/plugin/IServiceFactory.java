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
package org.hippoecm.frontend.plugin;

import org.apache.wicket.IClusterable;

/**
 * A service factory intercepts access to a service.  This allows the factory to create an implementation
 * specific to the plugin requesting access, e.g. to maintain additional state or to be notified of the plugin's
 * disappearance.
 *
 * @param <S> the service interface that is wrapped by the factory
 */
public interface IServiceFactory<S extends IClusterable> extends IClusterable {

    /**
     * The service class to be wrapped.  Needs to be be present here since the type parameter is not
     * available at run time.  (FIXME: really?)
     *
     * @return the service interface class
     */
    Class<? extends S> getServiceClass();

    /**
     * Invoked when the service is requested from the service registry with the provided plugin context.
     *
     * @param context the plugin context that was used to request access
     *
     * @return an instance of the service that can be used by the plugin
     */
    S getService(IPluginContext context);

    /**
     * When the plugin that requested an instance of the service, is stopped, this method is invoked.
     * The service factory is encouraged to clean up any resources that have been allocated on behalf
     * of the plugin.
     *
     * @param context the plugin context for the plugin that is stopped
     * @param service the instance of the service that was made available to the plugin
     */
    void releaseService(IPluginContext context, S service);

}
