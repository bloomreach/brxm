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
import java.util.Map;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginConfig;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.ServiceListener;

public class PluginContextImpl implements PluginContext, Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, Object> properties;
    private PluginManager manager;

    public PluginContextImpl(PluginManager manager, PluginConfig config) {
        this.manager = manager;
        this.properties = config;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public Plugin start(PluginConfig config) {
        return manager.start(config);
    }

    public void registerService(Serializable service, String name) {
        manager.registerService(service, name);
    }

    public void unregisterService(Serializable service, String name) {
        manager.unregisterService(service, name);
    }

    public void registerListener(ServiceListener listener, String name) {
        manager.registerListener(listener, name);
    }

    public void unregisterListener(ServiceListener listener, String name) {
        manager.unregisterListener(listener, name);
    }

}
