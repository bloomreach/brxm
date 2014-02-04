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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.ConfirmDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.TextDialog;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.reviewedactions.model.Request;
import org.hippoecm.frontend.plugins.reviewedactions.model.RequestModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.repository.documentworkflow.HandleDocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestsView extends RepeatingView {

    static final Logger log = LoggerFactory.getLogger(RequestsView.class);

    private static final long serialVersionUID = 1L;

    private final DateFormat dateFormatFull = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL,
            getSession().getLocale());
    private final IPluginContext context;

    public RequestsView(String id, WorkflowDescriptorModel model, IPluginContext context) {
        super(id, model);
        this.context = context;
        onPopulate();
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }

    @Override
    protected void onPopulate() {
        List<IModel<Request>> requests = new ArrayList<>();
        try {
            WorkflowDescriptorModel model = getModel();
            WorkflowManager manager = UserSession.get().getWorkflowManager();
            WorkflowDescriptor workflowDescriptor = model.getObject();
            if (workflowDescriptor != null) {
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();
                if (info.containsKey("requests")) {
                    Map<String, Map<String, ?>> infoRequests = (Map<String, Map<String, ?>>) info.get("requests");
                    for (Map.Entry<String, Map<String, ?>> entry : infoRequests.entrySet()) {
                        requests.add(new RequestModel(entry.getKey(), entry.getValue()));
                    }
                }
            }
        } catch (RepositoryException | WorkflowException | RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
        removeAll();
        int index = 0;
        for (IModel<Request> requestModel : requests) {
            Item<Request> item = new Item<>(newChildId(), index++, requestModel);
            populateItem(item);
            add(item);
        }
    }

    protected void populateItem(final Item<Request> item) {
        item.add(new StdWorkflow("info", "info") {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel getTitle() {
                final Request request = item.getModelObject();
                Date schedule = request.getSchedule();
                String state = request.getState();

                final String parameter = schedule != null ? dateFormatFull.format(schedule) : "??";
                return new StringResourceModel("state-" + state, this, null, "unknown", parameter);
            }

            @Override
            protected void invoke() {
            }
        });

        item.add(new StdWorkflow("accept", new StringResourceModel("accept-request", this, null), getModel()) {

            @Override
            public boolean isVisible() {
                final Request request = item.getModel().getObject();
                return Boolean.TRUE.equals(request.getAccept());
            }

            @Override
            public String getSubMenu() {
                return "request";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/workflow-accept-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                HandleDocumentWorkflow workflow = (HandleDocumentWorkflow) wf;
                workflow.acceptRequest(item.getModelObject().getId());
                return null;
            }
        });

        item.add(new StdWorkflow("reject", new StringResourceModel("reject-request", this, null), context, getModel()) {
            public String reason;

            @Override
            public boolean isVisible() {
                final Request request = item.getModel().getObject();
                return Boolean.TRUE.equals(request.getReject());
            }

            @Override
            public String getSubMenu() {
                return "request";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/workflow-requestunpublish-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                final StdWorkflow stdWorkflow = this;
                return new TextDialog(
                        new StringResourceModel("reject-request-title", RequestsView.this, null),
                        new StringResourceModel("reject-request-text", RequestsView.this, null),
                        new PropertyModel<String>(this, "reason")) {
                    @Override
                    public void invokeWorkflow() throws Exception {
                        stdWorkflow.invokeWorkflow();
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                HandleDocumentWorkflow workflow = (HandleDocumentWorkflow) wf;
                workflow.rejectRequest(item.getModelObject().getId(), reason);
                return null;
            }
        });

        item.add(new StdWorkflow("cancel", new StringResourceModel("cancel-request", this, null), context, getModel()) {

            @Override
            public boolean isVisible() {
                final Request request = item.getModel().getObject();
                return Boolean.TRUE.equals(request.getCancel());
            }

            @Override
            public String getSubMenu() {
                return "request";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/delete-16.png");
            }

            @Override
            protected IModel getTitle() {
                String state = getState();
                if (state.equals("rejected")) {
                    return new StringResourceModel("drop-request", RequestsView.this, null);
                } else {
                    return new StringResourceModel("cancel-request", RequestsView.this, null);
                }
            }

            private String getState() {
                final Request request = item.getModel().getObject();
                return request.getState();
            }

            @Override
            protected Dialog createRequestDialog() {
                String state = getState();
                if (state.equals("rejected")) {
                    IModel<String> reason = null;
                    try {
                        WorkflowDescriptorModel model = getModel();
                        Node node = (model != null ? model.getNode() : null);
                        if (node != null && node.hasProperty("hippostdpubwf:reason")) {
                            reason = Model.of(node.getProperty("hippostdpubwf:reason").getString());
                        }
                    } catch (RepositoryException ex) {
                        log.warn(ex.getMessage(), ex);
                    }
                    if (reason == null) {
                        reason = new StringResourceModel("rejected-request-unavailable", RequestsView.this, null);
                    }
                    final StdWorkflow cancelAction = this;
                    return new ConfirmDialog(
                            new StringResourceModel("rejected-request-title", RequestsView.this, null),
                            new StringResourceModel("rejected-request-text", RequestsView.this, null),
                            reason,
                            new StringResourceModel("rejected-request-question", RequestsView.this, null)) {

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
                HandleDocumentWorkflow workflow = (HandleDocumentWorkflow) wf;
                workflow.cancelRequest(item.getModelObject().getId());
                return null;
            }
        });

    }
}
