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
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;

public class FullRequestWorkflowPlugin extends CompatibilityWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    private String state = "unknown";
    private Date schedule = null;

    WorkflowAction acceptAction;
    WorkflowAction rejectAction;
    WorkflowAction cancelAction;
    
    public FullRequestWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        add(new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                return new StringResourceModel("state-"+state, this, null,
                    new Object[] { (schedule!=null ? dateFormatFull.format(schedule) : "??") }, "unknown");
            }
            @Override
            protected void invoke() {
            }
        });

        add(acceptAction = new WorkflowAction("accept", new StringResourceModel("accept-request", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "workflow-accept-16.png");
            }
            @Override
            protected String execute(Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.acceptRequest();
                return null;
            }
        });

        add(rejectAction = new WorkflowAction("reject", new StringResourceModel("reject-request", this, null).getString(), null) {
            public String reason;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "workflow-requestunpublish-16.png");
            }
            @Override
            protected Dialog createRequestDialog() {
                return new WorkflowAction.TextDialog(new StringResourceModel("reject-request-title",
                                                                             FullRequestWorkflowPlugin.this, null),
                                                     new StringResourceModel("reject-request-text",
                                                                             FullRequestWorkflowPlugin.this, null),
                                                     new PropertyModel(this, "reason"));
            }
            @Override
            protected String execute(Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.rejectRequest(reason);
                return null;
            }
        });

        add(cancelAction = new WorkflowAction("cancel", new StringResourceModel("cancel-request", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "delete-16.png");
            }
            @Override
            protected IModel getTitle() {
                return new StringResourceModel("cancel-request", FullRequestWorkflowPlugin.this, null);
            }
            @Override
            protected String execute(Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.cancelRequest();
                return null;
            }
        });

        schedule = null;
        state = "unknown";
        try {
            WorkflowManager manager = UserSession.get().getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel)getDefaultModel();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor)getDefaultModelObject();
            if (workflowDescriptor != null) {
                Node documentNode = workflowDescriptorModel.getNode();

                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();
                if (info.containsKey("acceptRequest") && info.get("acceptRequest") instanceof Boolean) {
                    acceptAction.setVisible((Boolean)info.get("acceptRequest"));
                }
                if (info.containsKey("rejectRequest") && info.get("rejectRequest") instanceof Boolean) {
                    rejectAction.setVisible((Boolean)info.get("rejectRequest"));
                }
                if (info.containsKey("cancelRequest") && info.get("cancelRequest") instanceof Boolean) {
                    cancelAction.setVisible((Boolean)info.get("cancelRequest"));
                } else {
                    cancelAction.setVisible(false);
                }

                if (documentNode.hasProperty("hippostdpubwf:type")) {
                    state = documentNode.getProperty("hippostdpubwf:type").getString();
                }
                if (documentNode.hasProperty("hipposched:triggers/default/hipposched:fireTime")) {
                    schedule = documentNode.getProperty("hipposched:triggers/default/hipposched:fireTime").getDate().getTime();
                } else if (documentNode.hasProperty("hippostdpubwf:reqdate")) {
                    schedule = new Date(documentNode.getProperty("hippostdpubwf:reqdate").getLong());
                }
            }
         } catch (WorkflowException ex) {
         } catch (RemoteException ex) {
         } catch (RepositoryException ex) {
            // unknown, maybe there are legit reasons for this, so don't emit a warning
        }
    }
}
