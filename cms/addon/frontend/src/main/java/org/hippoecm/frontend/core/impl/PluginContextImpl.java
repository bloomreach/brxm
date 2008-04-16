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
package org.hippoecm.frontend.core.impl;

import java.io.Serializable;
import java.util.List;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginConfig;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.ServiceListener;

public class PluginContextImpl implements PluginContext, Serializable {
    private static final long serialVersionUID = 1L;

    private PluginManager manager;
    private PluginConfig config;

    public PluginContextImpl(PluginManager manager, PluginConfig directory) {
        this.manager = manager;
        this.config = directory;
    }

    public Plugin start(PluginConfig config) {
        return manager.start(config);
    }

    public String getProperty(String key) {
        return config.get(key);
    }

    public List<Serializable> getServices(String name) {
        String full = config.get(name);
        if (full != null) {
            return manager.getServices(full);
        }
        return null;
    }

    public void registerService(Serializable service, String name) {
        String full = config.get(name);
        if (full != null) {
            manager.registerService(service, full);
        }
    }

    public void unregisterService(Serializable service, String name) {
        String full = config.get(name);
        if (full != null) {
            manager.unregisterService(service, full);
        }
    }

    public void registerListener(ServiceListener listener, String name) {
        String full = config.get(name);
        if (full != null) {
            manager.registerListener(config, listener, full);
        }
    }

    public void unregisterListener(ServiceListener listener, String name) {
        String full = config.get(name);
        if (full != null) {
            manager.unregisterListener(listener, full);
        }
    }

}
