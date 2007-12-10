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
package org.hippoecm.frontend.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.plugin.config.PluginConfig;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class PluginManager implements IClusterable {
    private static final long serialVersionUID = 1L;

    private PluginConfig pluginConfig;
    private List<Plugin> pluginRegistry;

    public PluginManager(PluginConfig pluginConfig) {
        this.pluginRegistry = new ArrayList<Plugin>();
        this.pluginConfig = pluginConfig;
    }

    public void registerPlugin(Plugin plugin) {
        pluginRegistry.add(plugin);
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public WorkflowManager getWorkflowManager() {
        try {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            return ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public void update(final AjaxRequestTarget target, final PluginEvent event) {
        Iterator<Plugin> plugins = new ArrayList(pluginRegistry).iterator();
        while (plugins.hasNext()) {
            Plugin plugin = plugins.next();
            Set<EventChannel> incoming = plugin.getDescriptor().getIncoming();
            incoming.retainAll(event.getChannels());
            if (!incoming.isEmpty()) {
                try {
                    updatePlugin(plugin, target, event);

                    Node node = event.getNodeModel(JcrEvent.NEW_MODEL).getNode();
                    if (node != null) {
                        PluginDescriptor descriptor = plugin.getDescriptor();

                        //TODO: add optional property 'workflowcategory' to 
                        //frontend plugin configuration nodes and use that instead of the plugin id.
                        WorkflowManager manager = getWorkflowManager();
                        String workflowCategory = descriptor.getPluginId();
                        WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(workflowCategory, node);

                        String newPluginClass;
                        if (workflowDescriptor != null) {
                            newPluginClass = workflowDescriptor.getRendererName();
                        } else {
                            String pluginId = descriptor.getPluginId();
                            newPluginClass = pluginConfig.getPlugin(pluginId).getClassName();
                        }

                        String currentPluginClass = plugin.getClass().getName();
                        if (!newPluginClass.equals(currentPluginClass)) {
                            Plugin parentPlugin = plugin.getParentPlugin();
                            if (parentPlugin != null) {
                                parentPlugin.removeChild(descriptor);
                                descriptor.setClassName(newPluginClass);
                                Plugin newPlugin = parentPlugin.addChild(descriptor);
                                updatePlugin(newPlugin, target, event);
                                target.addComponent(parentPlugin);
                            }
                        }
                    }
                } catch (RepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }
    }

    // privates

    private void updatePlugin(Plugin plugin, final AjaxRequestTarget target, final PluginEvent event) {
        plugin.update(target, event);

        //Plugins can have DialogWindows as children, 
        //these need to be updated to.
        plugin.visitChildren(DialogWindow.class, new IVisitor() {
            public Object component(Component component) {
                DialogWindow dialogWindow = (DialogWindow) component;
                dialogWindow.update(target, event);
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }

}
