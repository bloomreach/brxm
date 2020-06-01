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

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Marker interface for classes that can be instantiated as a plugin.
 * There has to be a constructor with signature <init>(IPluginContext, IPluginConfig);
 * <p>
 * In general, the services that are available to a plugin may come and go at any moment,
 * so the plugin lifecycle is rather minimal.
 * <p>
 * Plugin lifecycle:
 * <ul>
 * <li><b>Construction</b> - The plugin can retrieve and use services.  Services that
 * are registered during this phase will be made available after construction completes.
 * Trackers can also be registered, but they will not receive any notifications until
 * construction completes.  In this phase, the constructor with signature
 * ({@link IPluginContext}, {@link IPluginConfig}) is invoked.
 * <p>
 * A plugin can check its configuration and throw runtime exceptions when it finds that
 * it cannot function correctly.  For render plugins, a plugin that renders the error will be
 * instantiated in this case, informing the user of the problem.
 * 
 * <li><b>Connect</b> - Services that were registered during construction will be made
 * available.  Services can be invoked at this point.  Trackers are activated, and will
 * receive notifications for services that are already available in the system.
 * 
 * <li><b>Start</b> - The {@link #start()} method is invoked after services.  The plugin
 * can start new plugin clusters in this phase.
 * 
 * <li><b>Stop</b> - Before the framework starts cleanup of the registered services and
 * trackers, plugins can release resources by implementing {@link #stop()}.
 * 
 * <li><b>Destruction</b> - The framework unregisters any services and trackers that were
 * registered by the plugin.
 * </ul>
 */
public interface IPlugin extends IClusterable {

    String CLASSNAME = "plugin.class";

    /**
     * This method can be implemented by plugins to delay part of the initialization until
     * subclasses have finished their construction.
     */
    void start();

    /**
     * Release references to external resources.  It is not necessary to unregister services
     * or trackers; this is handled by the framework.
     */
    void stop();
}
