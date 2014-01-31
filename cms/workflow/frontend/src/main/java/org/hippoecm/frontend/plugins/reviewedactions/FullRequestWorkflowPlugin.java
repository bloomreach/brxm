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
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.TextDialog;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;
import org.hippoecm.repository.util.JcrUtils;

public class FullRequestWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private final DateFormat dateFormatFull = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL,
                                                                             getSession().getLocale());

    private String state = "unknown";
    private Date schedule = null;

    private final StdWorkflow acceptAction;
    private final StdWorkflow rejectAction;
    private final StdWorkflow cancelAction;
    
    public FullRequestWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                final String parameter = schedule != null ? dateFormatFull.format(schedule) : "??";
                return new StringResourceModel("state-" + state, this, null, "unknown", parameter);
            }

            @Override
            protected void invoke() {
            }
        });

        add(acceptAction = new StdWorkflow("accept", new StringResourceModel("accept-request", this, null), getModel()) {

            @Override
            public String getSubMenu() {
                return "request";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "workflow-accept-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.acceptRequest();
                return null;
            }
        });

        add(rejectAction = new StdWorkflow("reject", new StringResourceModel("reject-request", this, null), context, getModel()) {
            public String reason;

            @Override
            public String getSubMenu() {
                return "request";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "workflow-requestunpublish-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                return new TextDialog(
                        new StringResourceModel("reject-request-title", FullRequestWorkflowPlugin.this, null),
                        new StringResourceModel("reject-request-text", FullRequestWorkflowPlugin.this, null),
                        new PropertyModel<String>(this, "reason")) {
                    @Override
                    public void invokeWorkflow() throws Exception {
                        rejectAction.invokeWorkflow();
                    }
                };
            }
            @Override
            protected String execute(Workflow wf) throws Exception {
                FullRequestWorkflow workflow = (FullRequestWorkflow) wf;
                workflow.rejectRequest(reason);
                return null;
            }
        });

        add(cancelAction = new StdWorkflow("cancel", new StringResourceModel("cancel-request", this, null), getModel()) {

            @Override
            public String getSubMenu() {
                return "request";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "delete-16.png");
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
            WorkflowManager manager = getSession().getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = getModel();
            WorkflowDescriptor workflowDescriptor = workflowDescriptorModel.getObject();
            if (workflowDescriptor != null) {

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

                Node request = workflowDescriptorModel.getNode();
                final String refId = JcrUtils.getStringProperty(request, "hippostdpubwf:refId", null);
                if (refId != null) {
                    final Node handle = request.getSession().getNodeByIdentifier(refId).getParent();
                    request = handle.getNode("hippo:request");
                    state = "scheduled" + JcrUtils.getStringProperty(request, "hipposched:methodName", state);
                } else {
                    state = JcrUtils.getStringProperty(request, "hippostdpubwf:type", state);
                }
                if (request.hasProperty("hipposched:triggers/default/hipposched:nextFireTime")) {
                    schedule = request.getProperty("hipposched:triggers/default/hipposched:nextFireTime").getDate().getTime();
                } else if (request.hasProperty("hippostdpubwf:reqdate")) {
                    schedule = new Date(request.getProperty("hippostdpubwf:reqdate").getLong());
                }
            }
         } catch (WorkflowException ex) {
         } catch (RemoteException ex) {
         } catch (RepositoryException ex) {
            // unknown, maybe there are legit reasons for this, so don't emit a warning
        }
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }
}
