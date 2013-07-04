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
package org.hippoecm.frontend.plugin;

import java.util.List;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * The main interface for a plugin to communicate with other plugins.  It can be
 * used to register, retrieve and track services.   Furthermore, it gives plugins
 * the possibility to start clusters of new plugins.
 * <p>
 * In general, services may come and go at any time.  It is therefore not allowed to
 * hold on to references to services, unless a {@link IServiceTracker} is used to
 * clean up when the service disappears.
 * <p>
 * There are some exceptions to the above rule; some services are provided by the
 * framework and may therefore be assumed to always exist.  These are
 * <ul>
 * <li>a {@link IDialogService} with name <code>service.dialog</code>
 * <li>an observable registry that tracks {@link IObserver} services that register
 * under the name org.hippoecm.frontend.model.event.IObserver
 * </ul>
 */
public interface IPluginContext extends IClusterable {

    /**
     * Create a new cluster of plugins.  The template specifies what services are
     * provided, what services are used and what additional properties can be set
     * for the cluster.  The parameters specify the values that are required to
     * create a cluster from the template.
     * <p>
     * Note that clusters that are started during the construction phase will not
     * be able to reference services that have been created in the same phase.
     * 
     * @param template A cluster template, i.e. a configuration hierarchy whose
     *                 variables have not yet been expanded.  The template contains
     *                 values such as "${myparam}" that will be expanded.  
     * @param parameters The values for the variables in the template.
     */
    IClusterControl newCluster(IClusterConfig template, IPluginConfig parameters);

    /**
     * Retrieve a service.  The first service that registered under the service name
     * and that is an instance of the class, is returned.  Always use an interface
     * for the class name.
     */
    <T extends IClusterable> T getService(String name, Class<T> clazz);

    /**
     * Retrieve the full list of services that registered under the service name and
     * that are instances of the class.  The list is immutable and will not be updated
     * by the system.  To receive notifications when services (un)register, use an
     * {@link IServiceTracker}.
     */
    <T extends IClusterable> List<T> getServices(String name, Class<T> clazz);

    /**
     * Retrieve a reference that can be stored in a page that is different from the one
     * that contains the plugin.  It is necessary to use these when communicating across
     * pages, e.g. as in the case of a non-ajax modal window.
     * <p>
     * Since the service reference contains a unique id for the service, it can also be
     * used to construct unique service names.
     */
    <T extends IClusterable> IServiceReference<T> getReference(T service);

    /**
     * Registers a service with the given name.  A service can be registered under multiple
     * names.
     */
    void registerService(IClusterable service, String name);

    /**
     * Unregisters a service from the given name.  If the service has been registered
     * under other names as well, it will still be available under those.
     */
    void unregisterService(IClusterable service, String name);

    /**
     * Registers a service tracker with the given name.
     */
    void registerTracker(IServiceTracker<? extends IClusterable> listener, String name);

    /**
     * Unregisters a service tracker with the given name.
     */
    void unregisterTracker(IServiceTracker<? extends IClusterable> listener, String name);

}
