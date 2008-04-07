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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowDialogAction;
import org.hippoecm.frontend.plugins.standardworkflow.export.NamespaceUpdater;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.frontend.template.config.RepositoryTypeConfig;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow.TypeUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConversionWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    static protected Logger log = LoggerFactory.getLogger(ConversionWorkflowPlugin.class);

    public ConversionWorkflowPlugin(PluginDescriptor pluginDescriptor, final IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, (WorkflowsModel) model, parentPlugin);

        addWorkflowAction("convert", "Convert to new model", true, new WorkflowDialogAction() {
            private static final long serialVersionUID = 1L;

            public Request execute(Channel channel, Workflow wf) throws Exception {
                JcrSessionModel sessionModel = ((UserSession) Session.get()).getJcrSessionModel();
                WorkflowsModel workflowModel = (WorkflowsModel) getModel();

                Node node = workflowModel.getNodeModel().getNode();
                // FIXME: iterate over all drafts
                node = getDraft(node);

                String type = node.getPrimaryNodeType().getName();
                if (type.indexOf(':') < 0) {
                    return null;
                }
                final String namespace = type.substring(0, type.indexOf(':'));

                final String prefix;
                if (namespace.indexOf('_') > 0) {
                    prefix = namespace.substring(0, namespace.indexOf('_'));
                } else {
                    prefix = namespace;
                }

                NamespaceUpdater updater = new NamespaceUpdater(new RepositoryTypeConfig(RemodelWorkflow.VERSION_OLD) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public TypeDescriptor getTypeDescriptor(String type) {
                        if (type.indexOf(':') > 0) {
                            String typePrefix = type.substring(0, type.indexOf(':'));
                            if (prefix.equals(typePrefix)) {
                                return super.getTypeDescriptor(namespace + type.substring(type.indexOf(':')));
                            }
                        }
                        return super.getTypeDescriptor(type);
                    }
                }, new RepositoryTypeConfig(RemodelWorkflow.VERSION_CURRENT));
                Map<String, TypeUpdate> update = updater.getUpdate(namespace);

                sessionModel.getSession().save();

                log.info("remodelling namespace " + namespace);
                RemodelWorkflow workflow = (RemodelWorkflow) wf;

                workflow.convert(namespace, update);
                return null;
            }
        });
    }

    private Node getDraft(Node node) throws RepositoryException {
        NodeIterator children = node.getNodes(node.getName());
        while (children.hasNext()) {
            Node child = children.nextNode();
            if (child.isNodeType(HippoNodeType.HIPPO_REMODEL)) {
                String state = child.getProperty(HippoNodeType.HIPPO_REMODEL).getString();
                if ("draft".equals(state)) {
                    return child;
                }
            }
        }
        return null;
    }
}
