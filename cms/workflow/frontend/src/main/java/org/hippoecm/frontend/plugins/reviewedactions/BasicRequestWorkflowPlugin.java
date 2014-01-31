/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.ConfirmDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.reviewedactions.BasicRequestWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicRequestWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(BasicRequestWorkflowPlugin.class);

    private final DateFormat dateFormatFull = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL,
                                                                             getSession().getLocale());

    private String state = "unknown";
    private Date schedule = null;
    private boolean cancelable = true;

    StdWorkflow cancelAction;
    StdWorkflow infoAction;

    public BasicRequestWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("info", "info") {

            @Override
            protected IModel getTitle() {
                final String parameter = schedule!=null ? dateFormatFull.format(schedule) : "??";
                return new StringResourceModel("state-"+state, this, null, "unknown", parameter);
            }

            @Override
            protected void invoke() {
            }
        });

        add(cancelAction = new StdWorkflow("cancel", new StringResourceModel("cancel-request", this, null), context, getModel()) {

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "delete-16.png");
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
                        WorkflowDescriptorModel model = getModel();
                        Node node = (model != null ? model.getNode() : null);
                        if (node != null && node.hasProperty("hippostdpubwf:reason")) {
                            reason = Model.of(node.getProperty("hippostdpubwf:reason").getString());
                        }
                    } catch(RepositoryException ex) {
                        log.warn(ex.getMessage(), ex);
                    }
                    if (reason == null) {
                        reason = new StringResourceModel("rejected-request-unavailable", BasicRequestWorkflowPlugin.this, null);
                    }
                    return new ConfirmDialog(
                            new StringResourceModel("rejected-request-title", BasicRequestWorkflowPlugin.this, null),
                            new StringResourceModel("rejected-request-text", BasicRequestWorkflowPlugin.this, null),
                            reason,
                            new StringResourceModel("rejected-request-question", BasicRequestWorkflowPlugin.this,
                                                    null)) {
                        @Override
                        public void invokeWorkflow() throws Exception {
                            cancelAction.invokeWorkflow();
                        }
                    };
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

        WorkflowDescriptorModel model = getModel();
        schedule = null;
        try {
            Node node = model.getNode();
            state = node.getProperty("hippostdpubwf:type").getString();
            if (node.hasProperty("hipposched:triggers/default/hipposched:nextFireTime")) {
                schedule = node.getProperty("hipposched:triggers/default/hipposched:nextFireTime").getDate().getTime();
            } else if (node.hasProperty("hippostdpubwf:reqdate")) {
                schedule = new Date(node.getProperty("hippostdpubwf:reqdate").getLong());
            }
            Map<String, Serializable> hints = (model.getObject()).hints();
            if (hints.containsKey("cancelRequest") && !((Boolean)hints.get("cancelRequest")).booleanValue()) {
                cancelAction.setVisible(false);
            }
        } catch (RepositoryException ex) {
            // status unknown, maybe there are legit reasons for this, so don't emit a warning
            log.info(ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }

}
