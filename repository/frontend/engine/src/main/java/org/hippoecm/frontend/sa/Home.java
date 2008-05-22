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
package org.hippoecm.frontend.sa;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.sa.plugin.PluginManager;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.sa.plugin.config.impl.PluginConfigFactory;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.sa.service.IServiceListener;
import org.hippoecm.frontend.sa.service.PluginRequestTarget;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Home extends WebPage implements IServiceListener, IRenderService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(Home.class);

    private PluginManager mgr;
    private IRenderService root;

    public Home() {
        mgr = new PluginManager(this);
        mgr.registerListener(this, "service.root");

        JcrSessionModel sessionModel = ((UserSession) getSession()).getJcrSessionModel();
        PluginConfigFactory configFactory = new PluginConfigFactory(sessionModel);
        IPluginConfigService pluginConfigService = configFactory.getPluginConfigService();
        mgr.registerService(pluginConfigService, "service.plugin.config");
        
        List<IPluginConfig> plugins = pluginConfigService.getPlugins("default");
        for (IPluginConfig plugin : plugins) {
            mgr.start(plugin);
        }
    }

    private IRenderService getRootPlugin() {
        return root;
    }

    public void processEvent(int type, String name, IClusterable service) {
        switch (type) {
        case IServiceListener.ADDED:
            if (service instanceof IRenderService) {
                root = (IRenderService) service;
                root.bind(this, "root");
                add((Component) root);
            } else {
                log.error("root plugin is not a RenderService");
            }
            break;

        case IServiceListener.REMOVE:
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

    public String getServiceId() {
        return null;
    }

    public final PluginManager getPluginManager() {
        return mgr;
    }
}
