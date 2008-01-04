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
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.empty.EmptyPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(WorkflowPlugin.class);

    private Plugin plugin;

    public WorkflowPlugin(PluginDescriptor descriptor, JcrNodeModel model, Plugin parent) {
        super(descriptor, model, parent);

        PluginDescriptor childDescriptor = new PluginDescriptor("workflowPlugin", EmptyPlugin.class.getName(), null);
        plugin = addChild(childDescriptor);
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getData());

            try {
                Node node = model.getNode();
                if (node != null) {
                    PluginDescriptor descriptor = plugin.getDescriptor();

                    //TODO: add optional property 'workflowcategory' to
                    //frontend plugin configuration nodes and use that instead of the plugin id.
                    WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                    String workflowCategory = descriptor.getPluginId();
                    WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor(workflowCategory, node);

                    String newPluginClass;
                    if (workflowDescriptor != null) {
                        newPluginClass = workflowDescriptor.getRendererName();
                    } else {
                        newPluginClass = EmptyPlugin.class.getName();
                    }

                    String currentPluginClass = plugin.getClass().getName();
                    if (!newPluginClass.equals(currentPluginClass)) {
                        removeChild(descriptor);
                        descriptor.setClassName(newPluginClass);
                        plugin = addChild(descriptor);
                        notification.getContext().addRefresh(this);
                    }
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        super.receive(notification);
    }
}
