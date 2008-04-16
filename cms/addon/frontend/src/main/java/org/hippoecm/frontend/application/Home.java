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

import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.console.Application;
import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginConfig;
import org.hippoecm.frontend.core.ServiceListener;
import org.hippoecm.frontend.core.impl.PluginManager;
import org.hippoecm.frontend.wicket.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Home extends WebPage implements ServiceListener {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(Home.class);

    public static final String ROOT_PLUGIN = "rootPlugin";
//    public static final String LOGIN_PLUGIN = "loginPlugin";

    private PluginManager mgr;
    private RenderService root;

    public Home() {

        root = null;
        mgr = new PluginManager();

        PluginConfig config = new PluginConfig();
        config.put("root", "root");
        mgr.registerListener(config, this, "root");

        config = new PluginConfig();
        config.put(Plugin.NAME, "app");
        config.put(Plugin.CLASSNAME, Application.class.getName());
        /* Plugin application = */ mgr.start(config);
    }

    public RenderService getRootPlugin() {
        return root;
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

        case ServiceListener.REMOVED:
            if (service == root) {
                remove(root);
                root = null;
            }
            break;
        }
    }

}
