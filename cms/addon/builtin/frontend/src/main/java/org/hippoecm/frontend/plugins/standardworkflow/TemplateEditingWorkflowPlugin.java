/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.WorkflowAction;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEditingWorkflowPlugin extends AbstractWorkflowPlugin implements IValidateService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static protected Logger log = LoggerFactory.getLogger(TemplateEditingWorkflowPlugin.class);

    // FIXME: should this be non-transient?
    private transient boolean validated = false;
    private transient boolean isvalid = true;

    public TemplateEditingWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        if (config.getString(IValidateService.VALIDATE_ID) != null) {
            context.registerService(this, config.getString(IValidateService.VALIDATE_ID));
        } else {
            log.warn("No validator id {} defined", IValidateService.VALIDATE_ID);
        }

        addWorkflowAction("save", new StringResourceModel("save", this, null), new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void execute(Workflow workflow) throws Exception {
                WorkflowsModel model = (WorkflowsModel) getModel();
                final JcrNodeModel nodeModel = model.getNodeModel();
                if (nodeModel.getNode() != null) {
                    nodeModel.getNode().save();

                    IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
                    if (jcrService != null) {
                        jcrService.flush(nodeModel);
                    }
                } else {
                    log.error("Node does not exist");
                }
                close();
            }

        });
        AjaxLink link = new AjaxLink("revert") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                WorkflowsModel model = (WorkflowsModel) TemplateEditingWorkflowPlugin.this.getModel();
                JcrNodeModel nodeModel = model.getNodeModel();
                if (nodeModel.getNode() != null) {
                    try {
                        nodeModel.getNode().refresh(false);
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                    IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
                    if (jcrService != null) {
                        jcrService.flush(nodeModel);
                    }
                } else {
                    log.error("Node does not exist");
                }
                close();
            }
        };
        link.add(new Label("revert-label", new StringResourceModel("revert", this, null)));
        add(link);
    }

    public boolean hasError() {
        if (!validated) {
            validate();
        }
        return !isvalid;
    }

    public void validate() {
        validated = true;
        isvalid = true;
        try {
            Node node = ((WorkflowsModel) getModel()).getNodeModel().getNode();
            if (node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                NodeIterator ntNodes = node.getNode(HippoNodeType.HIPPO_NODETYPE)
                        .getNodes(HippoNodeType.HIPPO_NODETYPE);
                Node ntNode = null;
                while (ntNodes.hasNext()) {
                    Node child = ntNodes.nextNode();
                    if (!child.isNodeType(HippoNodeType.NT_REMODEL)) {
                        ntNode = child;
                        break;
                    }
                }
                if (ntNode != null) {
                    HashSet<String> paths = new HashSet<String>();
                    NodeIterator fieldIter = ntNode.getNodes(HippoNodeType.HIPPO_FIELD);
                    while (fieldIter.hasNext()) {
                        Node field = fieldIter.nextNode();
                        String path = field.getProperty(HippoNodeType.HIPPO_PATH).getString();
                        if (paths.contains(path)) {
                            error("Path " + path + " is used in multiple fields");
                            isvalid = false;
                        }
                        if (!path.equals("*")) {
                            paths.add(path);
                        }
                    }
                } else {
                    log.error("Draft nodetype not found");
                }
            } else {
                log.warn("Unknown node type {}", node.getPrimaryNodeType().getName());
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    protected void onModelChanged() {
        validated = false;
        super.onModelChanged();
    }

    private void close() {
        IPluginContext context = getPluginContext();
        IEditService viewer = context.getService(getPluginConfig().getString(IEditService.EDITOR_ID),
                IEditService.class);
        if (viewer != null) {
            String serviceId = context.getReference(viewer).getServiceId();
            IFactoryService factory = context.getService(serviceId, IFactoryService.class);
            if (factory != null) {
                factory.delete(viewer);
            }
        } else {
            log.warn("No editor service found");
        }
    }

}
