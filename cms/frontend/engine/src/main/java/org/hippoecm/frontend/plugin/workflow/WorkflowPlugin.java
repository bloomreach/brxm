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
package org.hippoecm.frontend.plugin.workflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.empty.EmptyPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(WorkflowPlugin.class);

    AbstractView view;

    public WorkflowPlugin(PluginDescriptor descriptor, IPluginModel model, Plugin parent) {
        super(descriptor, new JcrNodeModel(model), parent);

        view = new AbstractView("workflows", new WorkflowProvider(), this) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item item) {
                String category = (String) item.getModelObject();

                //TODO: add optional property 'workflowcategory' to
                //frontend plugin configuration nodes and use that instead of the plugin id.
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();

                String pluginClass = EmptyPlugin.class.getName();
                try {
                    Node node = ((JcrNodeModel) getPluginModel()).getNode();
                    WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(category, node);

                    if (workflowDescriptor != null) {
                        pluginClass = workflowDescriptor.getRendererName();
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }

                PluginDescriptor descriptor = new PluginDescriptor("workflow", pluginClass, null);
                PluginFactory pluginFactory = new PluginFactory(getPluginManager());
                item.add(pluginFactory.createPlugin(descriptor, getPluginModel(), WorkflowPlugin.this));
            }

            @Override
            public void destroyItem(Item item) {
                Plugin plugin = (Plugin) item.get("workflow");
                plugin.destroy();
                item.remove("workflow");
                super.destroyItem(item);
            }
        };
        add(view);
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getModel());
            if (!model.equals(getModel())) {
                setPluginModel(model);
                notification.getContext().addRefresh(this);
                view.populate();
            }
        }
        super.receive(notification);
    }
}
