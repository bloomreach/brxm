/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.application;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.console.Application;
import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.ServiceListener;
import org.hippoecm.frontend.core.impl.PluginConfig;
import org.hippoecm.frontend.core.impl.PluginManager;
import org.hippoecm.frontend.plugin.config.ConfigValue;
import org.hippoecm.frontend.service.IRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginPage extends WebPage implements ServiceListener, IRenderService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginPage.class);

    public static final String ROOT_PLUGIN = "rootPlugin";
    //    public static final String LOGIN_PLUGIN = "loginPlugin";

    private PluginManager mgr;
    private IRenderService root;

    public PluginPage() {

        root = null;
        mgr = new PluginManager(this);

        PluginConfig config = new PluginConfig();
        config.put("root", new ConfigValue("service.root"));
        mgr.registerListener(this, "service.root");

        config = new PluginConfig();
        config.put(Plugin.CLASSNAME, new ConfigValue(Application.class.getName()));
        /* Plugin application = */mgr.start(config);
    }

    private IRenderService getRootPlugin() {
        return root;
    }

    public void processEvent(int type, String name, Serializable service) {
        switch (type) {
        case ServiceListener.ADDED:
            if (service instanceof IRenderService) {
                root = (IRenderService) service;
                root.bind(this, "root");
                add((Component) root);
            } else {
                log.error("root plugin is not a RenderService");
            }
            break;

        case ServiceListener.REMOVE:
            if (service == root) {
                remove((Component) root);
                root.unbind();
                root = null;
            }
            break;
        }
    }

    public void render(PluginRequestTarget target) {
        IRenderService root = getRootPlugin();
        if (root != null) {
            root.render(target);
        }
    }

    public void focus(IRenderService child) {
    }

    public void bind(IRenderService parent, String wicketId) {
    }

    public void unbind() {
    }

    public IRenderService getParentService() {
        return null;
    }

    public String getDecoratorId() {
        return null;
    }

    public final PluginManager getPluginManager() {
        return mgr;
    }
}
