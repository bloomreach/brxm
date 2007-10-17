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
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrEvent;
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
        JcrNodeModel rootModel = JcrNodeModel.getRootModel();
        if (rootModel == null) {
            String message = "Cannot find repository root, no connection to server.";
            add(new ErrorPlugin("rootPlugin", null, message));
        } else {
            PluginConfig pluginConfig = new PluginConfigFactory().getPluginConfig();            
            PluginDescriptor rootPluginDescriptor = pluginConfig.getRoot();
            PluginFactory rootPluginFactory = new PluginFactory(rootPluginDescriptor);
            rootPlugin = rootPluginFactory.getPlugin(rootModel);

            add(rootPlugin);
            rootPlugin.addChildren(pluginConfig);
        }
    }
    

    public void update(final AjaxRequestTarget target, JcrEvent jcrEvent) {
        try {
            reconfigurePlugins(target, jcrEvent);
            updatePlugins(target, jcrEvent);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void reconfigurePlugins(final AjaxRequestTarget target, final JcrEvent jcrEvent) throws RepositoryException {
        Session session = ((UserSession) getSession()).getJcrSession();
        final WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        final PluginConfig pluginConfig = new PluginConfigFactory().getPluginConfig();

        rootPlugin.visitChildren(Plugin.class, new IVisitor() {
            public Object component(Component component) {
                Plugin plugin = (Plugin) component;

                WorkflowDescriptor descriptor = null;
                try {
                    if (jcrEvent.getModel().getNode() != null) {
                        descriptor = manager.getWorkflowDescriptor(plugin.getId(), jcrEvent.getModel().getNode());
                    }
                } catch (RepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (descriptor == null) {
                    PluginDescriptor defaultPluginDescriptor = pluginConfig.getPlugin(plugin.getPath());
                    if (!defaultPluginDescriptor.getClassName().equals(plugin.getClass().getName())) {
                        PluginFactory pluginFactory = new PluginFactory(defaultPluginDescriptor);
                        Plugin defaultPlugin = pluginFactory.getPlugin(jcrEvent.getModel());
                        replace(defaultPlugin);
                    }
                } else {
                    WorkflowPluginFactory pluginFactory = new WorkflowPluginFactory(manager, descriptor);
                    Plugin workflowPlugin = pluginFactory.getWorkflowPlugin(plugin.getId(), jcrEvent.getModel());
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

    private void updatePlugins(final AjaxRequestTarget target, final JcrEvent jcrEvent) {
        visitChildren(Plugin.class, new IVisitor() {
            public Object component(Component component) {
                Plugin plugin = (Plugin) component;
                plugin.update(target, jcrEvent);
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
        
        visitChildren(DialogWindow.class, new IVisitor() {
            public Object component(Component component) {
                DialogWindow dialogWindow = (DialogWindow) component;
                dialogWindow.setNodeModel(jcrEvent.getModel());
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }

}
