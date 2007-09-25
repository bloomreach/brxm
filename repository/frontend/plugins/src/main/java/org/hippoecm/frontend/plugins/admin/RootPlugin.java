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
package org.hippoecm.frontend.plugins.admin;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.WorkflowPluginFactory;
import org.hippoecm.frontend.plugin.config.PluginConfig;
import org.hippoecm.frontend.plugin.config.PluginConfigFactory;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class RootPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public RootPlugin(String id, JcrNodeModel model) {
        super(id, model);
    }

    public void update(final AjaxRequestTarget target, final JcrNodeModel model) {
        try {
            Session session = ((UserSession) getSession()).getJcrSession();
            final PluginConfig pluginConfig = new PluginConfigFactory().getPluginConfig();
            final WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();

            visitChildren(Plugin.class, new IVisitor() {
                public Object component(Component component) {
                    Plugin plugin = (Plugin) component;

                    WorkflowDescriptor descriptor = null;
                    try {
                        descriptor = manager.getWorkflowDescriptor(plugin.getId(), model.getNode());
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

                    if (!plugin.getRenderBodyOnly()) {
                        plugin.update(target, model);
                    }
                    return IVisitor.CONTINUE_TRAVERSAL;
                }
                
                private void replace(Plugin plugin) {
                    plugin.setRenderBodyOnly(true);
                    RootPlugin.this.replace(plugin);
                    target.addComponent(RootPlugin.this);
                }
            });
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //                    String newPluginClassname = model.getNode().getProperty(HippoNodeType.HIPPO_RENDERER).getString();
    //                    if (newPluginClassname != null) {
    //                        if (plugin.getId().equals("workflowPlugin")) {
    //                            PluginDescriptor pluginDescriptor = new PluginDescriptor(plugin.getPath(), newPluginClassname);
    //                            Plugin newPlugin = new PluginFactory(pluginDescriptor).getPlugin(model);
    //                            newPlugin.setRenderBodyOnly(true);
    //
    //                            RootPlugin.this.replace(newPlugin);
    //                            target.addComponent(RootPlugin.this);
    //                        }
    //                    }

    //                    } else {
    //                        Plugin emptyPlugin = new EmptyPlugin("workflowPlugin", model);
    //                        emptyPlugin.setRenderBodyOnly(true);
    //                        RootPlugin.this.replace(emptyPlugin);
    //                        target.addComponent(RootPlugin.this);
    //                    }

}
