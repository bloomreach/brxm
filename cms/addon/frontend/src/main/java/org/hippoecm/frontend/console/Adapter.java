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
package org.hippoecm.frontend.console;

import java.io.Serializable;

import org.hippoecm.frontend.core.ServiceListener;
import org.hippoecm.frontend.core.impl.PluginConfig;
import org.hippoecm.frontend.core.impl.PluginManager;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.config.ConfigValue;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Adapter extends Plugin implements ServiceListener {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(Adapter.class);

    private PluginManager mgr;
    private org.hippoecm.frontend.core.Plugin application;
    private RenderService root;

    public Adapter(PluginDescriptor descriptor, IPluginModel model, Plugin parentPlugin) {
        super(descriptor, model, parentPlugin);

        root = null;
        mgr = new PluginManager();

        PluginConfig config = new PluginConfig();
        config.put("root", new ConfigValue("root"));
        mgr.registerListener(this, "root");

        config = new PluginConfig();
        config.put(org.hippoecm.frontend.core.Plugin.CLASSNAME, new ConfigValue(Application.class.getName()));
        application = mgr.start(config);
    }

    @Override
    public void destroy() {
        application.stop();

        mgr.unregisterListener(this, "root");
        super.destroy();
    }

    public void processEvent(int type, String name, Serializable service) {
        switch (type) {
        case ServiceListener.ADDED:
            if (service instanceof RenderService) {
                root = (RenderService) service;
                add(root);
            } else {
                log.error("root plugin is not a RenderService");
            }
            break;

        case ServiceListener.REMOVE:
            if (service == root) {
                remove(root);
                root = null;
            }
            break;
        }
    }
}
