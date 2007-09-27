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
package org.hippoecm.frontend;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.WorkflowPluginFactory;
import org.hippoecm.frontend.plugin.config.PluginConfig;
import org.hippoecm.frontend.plugin.config.PluginConfigFactory;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class Home extends WebPage {
    private static final long serialVersionUID = 1L;

    private Plugin rootPlugin;

    public Home() {
        JcrNodeModel rootModel = getRootModel();
        if (rootModel == null) {
            String message = "Cannot find repository root, no connection to server.";
            add(new ErrorPlugin("rootPlugin", null, message));
        } else {
            PluginConfig pluginConfig = new PluginConfigFactory().getPluginConfig();
            rootPlugin = getRootPlugin(pluginConfig, rootModel);

            add(rootPlugin);
            rootPlugin.addChildren(pluginConfig);
        }
    }

    public void update(final AjaxRequestTarget target, final JcrNodeModel model) {
        try {
            Session session = ((UserSession) getSession()).getJcrSession();
            final PluginConfig pluginConfig = new PluginConfigFactory().getPluginConfig();
            final WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();

            if (manager.isConfigured()) {
                reconfigurePlugins(manager, model, pluginConfig, target);
            }
            updatePlugins(target, model);

        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void reconfigurePlugins(final WorkflowManager manager, final JcrNodeModel model,
            final PluginConfig pluginConfig, final AjaxRequestTarget target) {
        
        rootPlugin.visitChildren(Plugin.class, new IVisitor() {
            public Object component(Component component) {
                Plugin plugin = (Plugin) component;

                WorkflowDescriptor descriptor = null;
                try {
                    if (model.getNode() != null) {
                        descriptor = manager.getWorkflowDescriptor(plugin.getId(), model.getNode());
                    }
                } catch (RepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (descriptor == null) {
                    PluginDescriptor defaultPluginDescriptor = pluginConfig.getPlugin(plugin.getPath());
                    if (!defaultPluginDescriptor.getClassName().equals(plugin.getClass().getName())) {
                        PluginFactory pluginFactory = new PluginFactory(defaultPluginDescriptor);
                        Plugin defaultPlugin = pluginFactory.getPlugin(model);
                        replace(defaultPlugin);
                    }
                } else {
                    WorkflowPluginFactory pluginFactory = new WorkflowPluginFactory(manager, descriptor);
                    Plugin workflowPlugin = pluginFactory.getWorkflowPlugin(plugin.getId(), model);
                    replace(workflowPlugin);
                }
                return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }

            private void replace(Plugin plugin) {
                plugin.setRenderBodyOnly(true);
                rootPlugin.replace(plugin);
                target.addComponent(rootPlugin);
            }
        });
    }

    private void updatePlugins(final AjaxRequestTarget target, final JcrNodeModel model) {
        visitChildren(Plugin.class, new IVisitor() {
            public Object component(Component component) {
                Plugin plugin = (Plugin) component;
                if (!plugin.getRenderBodyOnly()) {
                    plugin.update(target, model);
                }
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }

    private JcrNodeModel getRootModel() {
        JcrNodeModel result;
        try {
            UserSession wicketSession = (UserSession) getSession();
            javax.jcr.Session jcrSession = wicketSession.getJcrSession();
            if (jcrSession == null) {
                result = null;
            } else {
                result = new JcrNodeModel(jcrSession.getRootNode());
            }
        } catch (RepositoryException e) {
            result = null;
        }
        return result;
    }

    private Plugin getRootPlugin(PluginConfig pluginConfig, JcrNodeModel model) {
        PluginDescriptor rootPluginDescriptor = pluginConfig.getRoot();
        PluginFactory rootPluginFactory = new PluginFactory(rootPluginDescriptor);
        return rootPluginFactory.getPlugin(model);
    }

}
