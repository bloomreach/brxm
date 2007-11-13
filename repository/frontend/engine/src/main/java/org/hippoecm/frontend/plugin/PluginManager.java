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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    private Map plugins;

    public PluginManager(PluginConfig pluginConfig) {
        this.plugins = new HashMap();
        this.pluginConfig = pluginConfig;
    }

    public void registerPlugin(Plugin plugin) {
        String id = plugin.getDescriptor().getPluginId();
        plugins.put(id, plugin);
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

    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        updateEventConsumers(target, jcrEvent);
        reconfigurePlugins(target, jcrEvent);
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    // privates

    private void reconfigurePlugins(AjaxRequestTarget target, JcrEvent jcrEvent) {
        Iterator it = plugins.values().iterator();

        while (it.hasNext()) {
            Plugin plugin = (Plugin) it.next();
            Plugin parentPlugin = plugin.getParentPlugin();
            Node node = jcrEvent.getModel().getNode();

            if (parentPlugin != null && node != null) {
                PluginDescriptor descriptor = plugin.getDescriptor();
                WorkflowManager manager = getWorkflowManager();

                try {
                    //TODO: add optional property 'workflowcategory' to 
                    //frontend plugin configuration nodes and use that instead of the plugin id.
                    String workflowCategory = plugin.getDescriptor().getPluginId();
                    WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(workflowCategory, node);

                    String newPluginClass;
                    if (workflowDescriptor != null) {
                        newPluginClass = workflowDescriptor.getRendererName();
                    } else {
                        String pluginId = plugin.getDescriptor().getPluginId();
                        newPluginClass = pluginConfig.getPlugin(pluginId).getClassName();
                    }

                    String currentPluginClass = plugin.getClass().getName();
                    if (!newPluginClass.equals(currentPluginClass)) {
                        parentPlugin.removeChild(descriptor);
                        descriptor.setClassName(newPluginClass);
                        parentPlugin.addChild(descriptor);
                        target.addComponent(parentPlugin);
                    }
                } catch (RepositoryException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateEventConsumers(final AjaxRequestTarget target, final JcrEvent jcrEvent) {
        Iterator it = plugins.values().iterator();
        while (it.hasNext()) {
            Plugin plugin = (Plugin) it.next();
            plugin.update(target, jcrEvent);

            //Plugins can have DialogWindows as children, 
            //these need to be updated to.
            plugin.visitChildren(DialogWindow.class, new IVisitor() {
                public Object component(Component component) {
                    DialogWindow dialogWindow = (DialogWindow) component;
                    dialogWindow.update(target, jcrEvent);
                    return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                }
            });
        }
    }

}
