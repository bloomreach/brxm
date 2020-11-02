/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.ConfirmDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.TextDialog;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.reviewedactions.model.Request;
import org.hippoecm.frontend.plugins.reviewedactions.model.RequestModel;
import org.hippoecm.frontend.plugins.standards.datetime.DateTimePrinter;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestsView extends RepeatingView {

    static final Logger log = LoggerFactory.getLogger(RequestsView.class);

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
            Workflow workflow = model.getWorkflow();
            if (workflow != null) {
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

        final Request request = item.getModel().getObject();

        final StdWorkflow requestInfo = new StdWorkflow("info", "info") {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel getTitle() {
                final Request request = item.getModelObject();
                Date schedule = request.getSchedule();
                String state = request.getState();

                final String parameter = schedule != null ?
                        DateTimePrinter.of(schedule).appendDST().print(FormatStyle.FULL) : "??";
                return new StringResourceModel("state-" + state, this)
                        .setDefaultValue("unknown")
                        .setParameters(parameter);
            }
        };
        item.add(requestInfo);
        setVisibility(requestInfo, request.getInfo());

        final StdWorkflow accept = new StdWorkflow("accept", new StringResourceModel("accept-request", this), getModel()) {

            @Override
            public String getSubMenu() {
                return "request";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.CHECK_CIRCLE);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                workflow.acceptRequest(item.getModelObject().getId());
                return null;
            }
        };
        item.add(accept);
        setVisibility(accept, request.getAccept());

        final StdWorkflow reject = new StdWorkflow("reject", new StringResourceModel("reject-request", this), context, getModel()) {
            @SuppressWarnings("unused") // reason is used in PropertyModel
            public String reason;

            @Override
            public String getSubMenu() {
                return "request";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.MINUS_CIRCLE);
            }

            @Override
            protected Dialog createRequestDialog() {
                final StdWorkflow stdWorkflow = this;
                return new TextDialog(
                        new StringResourceModel("reject-request-title", RequestsView.this),
                        new StringResourceModel("reject-request-text", RequestsView.this),
                        new PropertyModel<>(this, "reason")) {
                    @Override
                    public void invokeWorkflow() throws Exception {
                        stdWorkflow.invokeWorkflow();
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                workflow.rejectRequest(item.getModelObject().getId(), reason);
                return null;
            }
        };
        item.add(reject);
        setVisibility(reject, request.getReject());

        final StdWorkflow cancel = new StdWorkflow("cancel", new StringResourceModel("cancel-request", this), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "request";
            }


            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TIMES);
            }

            @Override
            protected IModel getTitle() {
                final String translationKey = isRequestRejected() ? "drop-request" : "cancel-request";
                return new StringResourceModel(translationKey, RequestsView.this);
            }

            private boolean isRequestRejected() {
                final Request request = item.getModel().getObject();
                final String state = request.getState();
                return state.equals("request-rejected");
            }

            @Override
            protected Dialog createRequestDialog() {
                if (isRequestRejected()) {
                    IModel<String> reason = null;
                    try {
                        String id = item.getModelObject().getId();
                        Node node = UserSession.get().getJcrSession().getNodeByIdentifier(id);
                        if (node != null && node.hasProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_REASON)) {
                            reason = Model.of(node.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_REASON).getString());
                        }
                    } catch (RepositoryException ex) {
                        log.warn(ex.getMessage(), ex);
                    }
                    if (reason == null) {
                        reason = new StringResourceModel("rejected-request-unavailable", RequestsView.this);
                    }
                    final StdWorkflow cancelAction = this;
                    final ConfirmDialog confirmDialog = new ConfirmDialog(
                            new StringResourceModel("rejected-request-title", RequestsView.this),
                            new StringResourceModel("rejected-request-text", RequestsView.this),
                            reason,
                            new StringResourceModel("rejected-request-question", RequestsView.this)) {

                        @Override
                        public void invokeWorkflow() throws Exception {
                            cancelAction.invokeWorkflow();
                        }
                    };
                    confirmDialog.add(ClassAttribute.append("rejected-request-dialog"));
                    return confirmDialog;
                } else {
                    return super.createRequestDialog();
                }
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                workflow.cancelRequest(item.getModelObject().getId());
                return null;
            }
        };
        item.add(cancel);
        setVisibility(cancel, request.getCancel());

    }

    private Component setVisibility(final StdWorkflow requestInfo, final Boolean info) {
        return requestInfo.setVisible(Boolean.TRUE.equals(info));
    }
}
