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
package org.hippoecm.frontend.sa.plugin.impl;

import java.util.List;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.sa.Home;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.IPluginControl;
import org.hippoecm.frontend.sa.plugin.PluginManager;
import org.hippoecm.frontend.sa.plugin.config.IClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.IServiceTracker;
import org.hippoecm.frontend.sa.service.ServiceReference;

public class PluginContext implements IPluginContext, IClusterable {
    private static final long serialVersionUID = 1L;

    private Home page;
    private IPluginConfig properties;
    private transient PluginManager manager = null;

    public PluginContext(Home page, IPluginConfig config) {
        this.page = page;
        this.properties = config;
    }

    public IPluginConfig getProperties() {
        return properties;
    }

    public IPluginControl start(IClusterConfig cluster) {
        PluginManager mgr = getManager();
        final IPluginControl[] controls = new IPluginControl[cluster.getPlugins().size()];
        int i = 0;
        for(IPluginConfig config : cluster.getPlugins()) {
            controls[i++] = mgr.start(config);
        }

        return new IPluginControl() {
            private static final long serialVersionUID = 1L;

            public void stopPlugin() {
                for(IPluginControl control : controls) {
                    control.stopPlugin();
                }
            }
        };
    }

    public <T extends IClusterable> T getService(String name) {
        return (T) getManager().getService(name);
    }

    public <T extends IClusterable> List<T> getServices(String name) {
        return getManager().getServices(name);
    }

    public <T extends IClusterable> ServiceReference<T> getReference(T service) {
        return getManager().getReference(service);
    }

    public void registerService(IClusterable service, String name) {
        getManager().registerService(service, name);
    }

    public void unregisterService(IClusterable service, String name) {
        getManager().unregisterService(service, name);
    }

    public void registerTracker(IServiceTracker listener, String name) {
        getManager().registerTracker(listener, name);
    }

    public void unregisterTracker(IServiceTracker listener, String name) {
        getManager().unregisterTracker(listener, name);
    }

    private PluginManager getManager() {
        if (manager == null) {
            manager = page.getPluginManager();
        }
        return manager;
    }
}
