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

import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.PluginConfig;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.WorkflowMappingException;

public abstract class Plugin extends Panel implements EventConsumer {

    private PluginManager pluginManager;
    private PluginDescriptor pluginDescriptor;
    private Plugin parentPlugin;
    private JcrNodeModel model;
    
    public Plugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor.getWicketId(), null);
        setOutputMarkupId(true);
        this.parentPlugin = parentPlugin;
        this.pluginDescriptor = pluginDescriptor;
        this.model = model;
    }

    public PluginDescriptor getDescriptor() {
        return pluginDescriptor;
    }

    public PluginManager getPluginManager() {
        if (pluginManager == null) {
            return parentPlugin.getPluginManager();
        }
        return pluginManager;
    }
    
    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public Plugin getParentPlugin() {
        return parentPlugin;
    }
    
    public JcrNodeModel getNodeModel() {
        return model;
    }
    
    public void setNodeModel(JcrNodeModel model) {
        this.model = model;
    }

    public void addChildren() {
        PluginDescriptor descriptor = getDescriptor();
        PluginConfig pluginConfig = getPluginManager().getPluginConfig();
        List children = pluginConfig.getChildren(descriptor);
        Iterator it = children.iterator();
        while (it.hasNext()) {
            PluginDescriptor childDescriptor = (PluginDescriptor) it.next();
            Plugin child = addChild(childDescriptor);
            child.addChildren();
        }
    }
    
    public Plugin addChild(PluginDescriptor childDescriptor) {
        PluginFactory pluginFactory = new PluginFactory(getPluginManager());
        Plugin child = pluginFactory.createPlugin(childDescriptor, model, this);

        add(child);
        return child;
    }

    public void removeChild(PluginDescriptor childDescriptor) {
        remove(childDescriptor.getWicketId());
    }
    
    protected Workflow getWorkflow() {
        Workflow workflow = null;
        try {
            WorkflowManager manager = getPluginManager().getWorkflowManager();
            WorkflowDescriptor descriptor = manager.getWorkflowDescriptor(getId(), model.getNode());
            workflow = manager.getWorkflow(descriptor);
        } catch (WorkflowMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return workflow;
    }

    public void update(AjaxRequestTarget target, JcrEvent event) {
        setNodeModel(event.getModel());
    }
}
