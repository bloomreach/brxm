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
package org.hippoecm.frontend.adapter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.legacy.model.PluginModel;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.PluginFactory;
import org.hippoecm.frontend.legacy.plugin.PluginManager;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.MessageContext;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.legacy.plugin.config.PluginConfig;
import org.hippoecm.frontend.legacy.plugin.config.PluginConfigFactory;
import org.hippoecm.frontend.legacy.plugin.config.PluginRepositoryConfig;
import org.hippoecm.frontend.legacy.template.config.JcrTypeConfig;
import org.hippoecm.frontend.legacy.template.config.MixedTypeConfig;
import org.hippoecm.frontend.legacy.template.config.RepositoryTemplateConfig;
import org.hippoecm.frontend.legacy.template.config.RepositoryTypeConfig;
import org.hippoecm.frontend.legacy.template.config.TemplateConfig;
import org.hippoecm.frontend.legacy.template.config.TypeConfig;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.PluginRequestTarget;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;

/**
 * Needed for handling legacy plugins
 * remove when all legacy plugins have been ported to new services architecture  
 */
public class Adapter extends Panel implements IRenderService, IModelListener, IJcrNodeModelListener {
    private static final long serialVersionUID = 1L;

    private org.hippoecm.frontend.legacy.plugin.Plugin rootPlugin;
    private IPluginContext context;
    private IPluginConfig config;
    private IRenderService parent;
    private String wicketId;
    private List<MessageContext> updates;

    public Adapter() {
        super("id");

        updates = new LinkedList<MessageContext>();
    }

    public org.hippoecm.frontend.legacy.plugin.Plugin getRootPlugin() {
        return rootPlugin;
    }

    public void init(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        TypeConfig repoTypeConfig = new RepositoryTypeConfig(RemodelWorkflow.VERSION_CURRENT);
        TypeConfig jcrTypeConfig = new JcrTypeConfig();
        List<TypeConfig> configs = new LinkedList<TypeConfig>();
        configs.add(repoTypeConfig);
        configs.add(jcrTypeConfig);
        TypeConfig typeConfig = new MixedTypeConfig(configs);

        TemplateConfig templateConfig = new RepositoryTemplateConfig();
        PluginConfigFactory configFactory = new PluginConfigFactory();
        PluginConfig pluginConfig = configFactory.getPluginConfig();

        PluginManager pluginManager = new PluginManager(pluginConfig, typeConfig, templateConfig);
        PluginFactory pluginFactory = new PluginFactory(pluginManager);

        String base = config.getString("legacy.base");
        String name = config.getString("legacy.plugin");
        PluginRepositoryConfig repoConfig = new PluginRepositoryConfig(base);
        PluginDescriptor descriptor = repoConfig.getPlugin(name);
        descriptor.setWicketId("legacyPlugin");

        JcrNodeModel rootModel;
        if (config.getString("wicket.model") != null) {
            IModelService modelService = context.getService(config.getString("wicket.model"), IModelService.class);
            if (modelService != null) {
                rootModel = (JcrNodeModel) modelService.getModel();
                if (rootModel == null) {
                    rootModel = new JcrNodeModel("/");
                }
            } else {
                UserSession session = (UserSession) Session.get();
                HippoNode rootNode = session.getRootNode();
                rootModel = new JcrNodeModel(rootNode);
            }
            context.registerService(this, config.getString("wicket.model"));
        } else {
            UserSession session = (UserSession) Session.get();
            HippoNode rootNode = session.getRootNode();
            rootModel = new JcrNodeModel(rootNode);
        }

        final PluginDescriptor childDescriptor = descriptor;
        PluginDescriptor rootDescriptor = new PluginDescriptor("adapted", RootPlugin.class.getName()) {
            private static final long serialVersionUID = 1L;

            @Override
            public List<PluginDescriptor> getChildren() {
                List<PluginDescriptor> list = new ArrayList<PluginDescriptor>();
                list.add(childDescriptor);
                return list;
            }
        };
        rootPlugin = pluginFactory.createPlugin(rootDescriptor, rootModel, null);
        rootPlugin.setPluginManager(pluginManager);

        String style = configFactory.getStyle();
        if (style != null) {
            add(HeaderContributor.forCss(style));
        }

        add(rootPlugin);
        rootPlugin.addChildren();

        context.registerService(this, config.getString("wicket.id"));

        context.registerService(this, IJcrService.class.getName());
    }

    public void destroy() {
        context.unregisterService(this, config.getString("wicket.id"));

        rootPlugin.destroy();
    }

    @Override
    public String getId() {
        return wicketId;
    }

    public Component getComponent() {
        return this;
    }

    public void bind(IRenderService parent, String id) {
        this.parent = parent;
        wicketId = id;
    }

    public void focus(IRenderService child) {
    }

    public IRenderService getParentService() {
        return parent;
    }

    public void render(PluginRequestTarget target) {
        if (findParent(Page.class) == null) {
            Channel channel = rootPlugin.getBottomChannel();
            PluginModel model = new PluginModel();
            model.put("plugin", "invalid path");
            Notification notification = channel.createNotification("focus", model);
            channel.publish(notification);
            updates.add(notification.getContext());
        }
        for (MessageContext context : updates) {
            context.apply(target);
        }
        updates = new LinkedList<MessageContext>();
    }

    public void unbind() {
        // TODO Auto-generated method stub

    }

    @Override
    public String toString() {
        return "Adapter";
    }

    public void updateModel(IModel model) {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel nodeModel = (JcrNodeModel) model;
            org.hippoecm.frontend.legacy.plugin.Plugin plugin = (org.hippoecm.frontend.legacy.plugin.Plugin) rootPlugin
                    .get("legacyPlugin");
            Channel top = plugin.getTopChannel();
            Notification notification = top.createNotification("select", nodeModel);
            top.publish(notification);
        }
    }

    public void onFlush(JcrNodeModel nodeModel) {
        Channel channel = rootPlugin.getBottomChannel();
        Notification notification = channel.createNotification("flush", nodeModel);
        channel.publish(notification);
        updates.add(notification.getContext());
    }

}
