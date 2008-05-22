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

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.sa.Home;
import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.PluginManager;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.IServiceListener;
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

    public IPlugin start(IPluginConfig config) {
        return getManager().start(config);
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

    public void registerListener(IServiceListener listener, String name) {
        getManager().registerListener(listener, name);
    }

    public void unregisterListener(IServiceListener listener, String name) {
        getManager().unregisterListener(listener, name);
    }

    private PluginManager getManager() {
        if (manager == null) {
            manager = page.getPluginManager();
        }
        return manager;
    }
}
