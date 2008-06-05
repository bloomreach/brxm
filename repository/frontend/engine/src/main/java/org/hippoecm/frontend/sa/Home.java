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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.IServiceTracker;
import org.hippoecm.frontend.sa.plugin.config.IClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaConfigService;
import org.hippoecm.frontend.sa.plugin.config.impl.PluginConfigFactory;
import org.hippoecm.frontend.sa.plugin.impl.PluginManager;
import org.hippoecm.frontend.sa.service.IJcrService;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.sa.service.PluginRequestTarget;
import org.hippoecm.frontend.sa.service.ServiceTracker;
import org.hippoecm.frontend.session.UserSession;

public class Home extends WebPage implements IServiceTracker<IRenderService>, IRenderService {
    private static final long serialVersionUID = 1L;

    private PluginManager mgr;
    private IRenderService root;
    private IPluginConfigService pluginConfigService;
    private List<IPluginContext> contexts;

    public Home() {
        add(new EmptyPanel("root"));

        mgr = new PluginManager(this);
        mgr.registerTracker(this, "service.root");

        JcrSessionModel sessionModel = ((UserSession) getSession()).getJcrSessionModel();
        PluginConfigFactory configFactory = new PluginConfigFactory(sessionModel);
        pluginConfigService = configFactory.getPluginConfigService();
        mgr.registerService(pluginConfigService, "service.plugin.config");

        // register JCR service to notify plugins of updates to the jcr tree
        mgr.registerService(new JcrService(mgr), IJcrService.class.getName());

        IClusterConfig pluginCluster;
        if (sessionModel.getCredentials().equals(Main.DEFAULT_CREDENTIALS)) {
            pluginCluster = new JavaConfigService().getPlugins("login");
        } else {
            pluginCluster = pluginConfigService.getDefaultCluster();
        }

        mgr.registerService(this, Home.class.getName());
        String serviceId = mgr.getReference(this).getServiceId();
        ServiceTracker<IBehavior> tracker = new ServiceTracker<IBehavior>(IBehavior.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onServiceAdded(IBehavior behavior, String name) {
                add(behavior);
            }

            @Override
            public void onRemoveService(IBehavior behavior, String name) {
                remove(behavior);
            }
        };
        mgr.registerTracker(tracker, serviceId);

        List<IPluginConfig> configs = pluginCluster.getPlugins();
        contexts = new ArrayList<IPluginContext>(configs.size());
        for (IPluginConfig plugin : configs) {
            contexts.add(mgr.start(plugin, serviceId));
        }
    }

    public Component getComponent() {
        return this;
    }

    public void render(PluginRequestTarget target) {
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

    // DO NOT CALL THIS METHOD
    // Use the IPluginContext to access the plugin manager
    public final PluginManager getPluginManager() {
        return mgr;
    }

    public void addService(IRenderService service, String name) {
        root = service;
        root.bind(this, "root");
        replace(root.getComponent());
    }

    public void removeService(IRenderService service, String name) {
        replace(new EmptyPanel("root"));
        root.unbind();
        root = null;
    }

    public void updateService(IRenderService service, String name) {
    }

    @Override
    public void onDetach() {
        mgr.detach();
        for (IPluginContext context : contexts) {
            context.detach();
        }
        super.onDetach();
    }

}
