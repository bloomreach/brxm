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

import java.util.Iterator;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.sa.core.IServiceListener;
import org.hippoecm.frontend.sa.core.impl.PluginConfig;
import org.hippoecm.frontend.sa.core.impl.PluginManager;
import org.hippoecm.frontend.sa.plugin.config.JavaConfigService;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginPage extends WebPage implements IServiceListener, IRenderService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginPage.class);

    public static final ValueMap ANONYMOUS_CREDENTIALS = new ValueMap("username=admin,password=admin");

    public static final String ROOT_PLUGIN = "rootPlugin";
    public static final String LOGIN_PLUGIN = "loginPlugin";

    private PluginManager mgr;
    private IRenderService root;

    public PluginPage() {

        root = null;
        mgr = new PluginManager(this);

        mgr.registerListener(this, "service.root");

        UserSession session = (UserSession) getSession();
        if (session.getCredentials().equals(ANONYMOUS_CREDENTIALS)) {
            PluginConfig config = new PluginConfig();
            config.put("plugin.class", "org.hippoecm.frontend.plugins.admin.login.sa.LoginPlugin");
            config.put("wicket.id", "service.root");
            mgr.start(config);

        } else {
            JavaConfigService configuration = new JavaConfigService();
            Iterator<PluginConfig> iter = configuration.getPlugins().iterator();
            while (iter.hasNext()) {
                mgr.start(iter.next());
            }
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
