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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.ResourceReference;

import org.apache.wicket.model.IModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.reviewedactions.BasicRequestWorkflow;

public class BasicRequestWorkflowPlugin extends CompatibilityWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    private String state = "unknown";
    private Date schedule = null;
    private boolean cancelable = true;

    WorkflowAction cancelAction;
    WorkflowAction infoAction;

    public BasicRequestWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                return new StringResourceModel("state-"+state, this, null,
                    new Object[] {  (schedule!=null ? dateFormatFull.format(schedule) : "??") }, "unknown");
            }
            @Override
            protected void invoke() {
            }
        });

        add(cancelAction = new WorkflowAction("cancel", new StringResourceModel("cancel-request", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "delete-16.png");
            }
            @Override
            protected IModel getTitle() {
                if (state.equals("rejected")) {
                    return new StringResourceModel("drop-request", BasicRequestWorkflowPlugin.this, null);
                } else {
                    return new StringResourceModel("cancel-request", BasicRequestWorkflowPlugin.this, null);
                }
            }
            @Override
            protected Dialog createRequestDialog() {
                if (state.equals("rejected")) {
                    IModel reason = null;
                    try {
                        if (getModel() instanceof WorkflowDescriptorModel) {
                            WorkflowDescriptorModel model = (WorkflowDescriptorModel)getModel();
                            Node node = (model != null ? model.getNode() : null);
                            if (node != null && node.hasProperty("hippostdpubwf:reason")) {
                                reason = new Model(node.getProperty("hippostdpubwf:reason").getString());
                            }
                        }
                    } catch(RepositoryException ex) {
                        log.warn(ex.getMessage(), ex);
                    }
                    if (reason == null) {
                        reason = new StringResourceModel("rejected-request-unavailable", BasicRequestWorkflowPlugin.this, null);
                    }
                    return new WorkflowAction.ConfirmDialog(new StringResourceModel("rejected-request-title",
                                                                                    BasicRequestWorkflowPlugin.this, null),
                                                            new StringResourceModel("rejected-request-text",
                                                                                    BasicRequestWorkflowPlugin.this, null),
                                                            reason,
                                                            new StringResourceModel("rejected-request-question",
                                                                                    BasicRequestWorkflowPlugin.this, null));
                } else {
                    return super.createRequestDialog();
                }
            }
            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicRequestWorkflow workflow = (BasicRequestWorkflow) wf;
                workflow.cancelRequest();
                return null;
            }
        });
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
        schedule = null;
        if (model != null) {
            try {
                Node node = model.getNode();
                state = node.getProperty("hippostdpubwf:type").getString();
                if (node.hasProperty("hipposched:triggers/default/hipposched:fireTime")) {
                    schedule = node.getProperty("hipposched:triggers/default/hipposched:fireTime").getDate().getTime();
                } else if (node.hasProperty("hippostdpubwf:reqdate")) {
                    schedule = new Date(node.getProperty("hippostdpubwf:reqdate").getLong());
                }
                Map<String, Serializable> hints = ((WorkflowDescriptor)model.getObject()).hints();
                if (hints.containsKey("cancelRequest") && !((Boolean)hints.get("cancelRequest")).booleanValue()) {
                    cancelAction.setVisible(false);
                }
            } catch (RepositoryException ex) {
                // status unknown, maybe there are legit reasons for this, so don't emit a warning
                log.info(ex.getClass().getName() + ": " + ex.getMessage());
            }
        }
    }
}
