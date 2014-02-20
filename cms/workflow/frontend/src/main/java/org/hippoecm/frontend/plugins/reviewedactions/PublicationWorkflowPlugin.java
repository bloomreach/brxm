/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.DepublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.ScheduleDepublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.SchedulePublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.UnpublishedReferencesDialog;
import org.hippoecm.frontend.plugins.reviewedactions.model.ReferenceProvider;
import org.hippoecm.frontend.plugins.reviewedactions.model.UnpublishedReferenceProvider;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.Workflow;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

public class PublicationWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    private StdWorkflow publishAction;

    public PublicationWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final StdWorkflow depublishAction;
        add(depublishAction = new StdWorkflow("depublish", new StringResourceModel("depublish-label", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/unpublish-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                final IModel docName = getDocumentName();
                IModel title = new StringResourceModel("depublish-title", PublicationWorkflowPlugin.this, null, docName);
                IModel message = new StringResourceModel("depublish-message", PublicationWorkflowPlugin.this, null, docName);
                return new DepublishDialog(title, message, getModel(), this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                workflow.depublish();
                return null;
            }
        });

        final StdWorkflow requestDepublishAction;
        add(requestDepublishAction = new StdWorkflow("requestDepublication", new StringResourceModel("request-depublication", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/workflow-requestunpublish-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                final IModel docName = getDocumentName();
                IModel<String> title = new StringResourceModel("depublish-title", PublicationWorkflowPlugin.this, null, docName);
                IModel<String> message = new StringResourceModel("depublish-message", PublicationWorkflowPlugin.this, null, docName);
                return new DepublishDialog(title, message, getModel(), this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                workflow.requestDepublication();
                return null;
            }
        });

        final StdWorkflow scheduleDepublishAction;
        add(scheduleDepublishAction = new StdWorkflow("scheduleDepublish", new StringResourceModel(
                "schedule-depublish-label", this, null), context, getModel()) {
            public Date date = new Date();

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/unpublish-scheduled-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                try {
                    return new ScheduleDepublishDialog(this, new JcrNodeModel(wdm.getNode()), new PropertyModel<Date>(this, "date"), getEditorManager());
                } catch (RepositoryException e) {
                    log.warn("could not retrieve node for scheduling depublish", e);
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                if (date != null) {
                    workflow.depublish(date);
                } else {
                    workflow.depublish();
                }
                return null;
            }
        });

        final StdWorkflow requestScheduleDepublishAction;
        add(requestScheduleDepublishAction = new StdWorkflow("requestScheduleDepublish", new StringResourceModel("schedule-request-depublish", this, null), context, getModel()) {
            public Date date = new Date();

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/unpublish-scheduled-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                try {
                    return new ScheduleDepublishDialog(this, new JcrNodeModel(wdm.getNode()), new PropertyModel<Date>(this, "date"), getEditorManager());
                } catch (RepositoryException e) {
                    log.warn("could not retrieve node for scheduling depublish", e);
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                if (date != null) {
                    workflow.requestDepublication(date);
                } else {
                    workflow.requestDepublication();
                }
                return null;
            }
        });


        add(publishAction = new StdWorkflow("publish", new StringResourceModel("publish-label", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/publish-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                try {
                    Node handle = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    Node unpublished = getVariant(handle, HippoStdNodeType.UNPUBLISHED);
                    final UnpublishedReferenceProvider referenced = new UnpublishedReferenceProvider(
                            new ReferenceProvider(new JcrNodeModel(unpublished)));
                    if (referenced.size() > 0) {
                        return new UnpublishedReferencesDialog(publishAction, new UnpublishedReferenceNodeProvider(
                                referenced), getEditorManager());
                    }
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                workflow.publish();
                return null;
            }
        });

        final StdWorkflow requestPublishAction;
        add(requestPublishAction = new StdWorkflow("requestPublication", new StringResourceModel("request-publication", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/workflow-requestpublish-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                HippoNode node;
                try {
                    node = (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    final UnpublishedReferenceProvider referenced = new UnpublishedReferenceProvider(new ReferenceProvider(
                            new JcrNodeModel(node)));
                    if (referenced.size() > 0) {
                        return new UnpublishedReferencesDialog(publishAction, new UnpublishedReferenceNodeProvider(referenced), getEditorManager());
                    }
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                workflow.requestPublication();
                return null;
            }
        });

        final StdWorkflow schedulePublishAction;
        add(schedulePublishAction = new StdWorkflow("schedulePublish", new StringResourceModel(
                "schedule-publish-label", this, null), context, getModel()) {
            public Date date = new Date();

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/publish-schedule-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                try {
                    Node unpublished = getVariant(wdm.getNode(), HippoStdNodeType.UNPUBLISHED);
                    return new SchedulePublishDialog(this, new JcrNodeModel(unpublished), new PropertyModel<Date>(this, "date"), getEditorManager());
                } catch (RepositoryException ex) {
                    log.warn("could not retrieve node for scheduling publish", ex);
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                if (date != null) {
                    workflow.publish(date);
                } else {
                    workflow.publish();
                }
                return null;
            }
        });

        final StdWorkflow requestSchedulePublishAction;
        add(requestSchedulePublishAction = new StdWorkflow("requestSchedulePublish", new StringResourceModel("schedule-request-publish", this, null), context, getModel()) {
            public Date date = new Date();

            @Override
            public String getSubMenu() {
                return "publication";
            }

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "img/publish-schedule-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = getModel();
                try {
                    Node unpublished = getVariant(wdm.getNode(), HippoStdNodeType.UNPUBLISHED);
                    return new SchedulePublishDialog(this, new JcrNodeModel(unpublished), new PropertyModel<Date>(this, "date"), getEditorManager());
                } catch (RepositoryException ex) {
                    log.warn("could not retrieve node for scheduling publish", ex);
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                if (date != null) {
                    workflow.requestPublication(date);
                } else {
                    workflow.requestPublication();
                }
                return null;
            }
        });


        Map<String, Serializable> info = getHints();

        if (isActionAllowed(info, "publish") ||
                isActionAllowed(info, "depublish") ||
                isActionAllowed(info, "requestPublication") ||
                isActionAllowed(info, "requestDepublication"))
        {
            hideOrDisable(info, "publish", publishAction, schedulePublishAction);
            hideOrDisable(info, "depublish", depublishAction, scheduleDepublishAction);

            if (!info.containsKey("publish")) {
                hideOrDisable(info, "requestPublication", requestPublishAction, requestSchedulePublishAction);
            } else {
                requestPublishAction.setVisible(false);
                requestSchedulePublishAction.setVisible(false);
            }

            if (!info.containsKey("depublish")) {
                hideOrDisable(info, "requestDepublication", requestDepublishAction, requestScheduleDepublishAction);
            } else {
                requestDepublishAction.setVisible(false);
                requestScheduleDepublishAction.setVisible(false);
            }
        }
        else {
            publishAction.setVisible(false);
            depublishAction.setVisible(false);
            requestPublishAction.setVisible(false);
            requestSchedulePublishAction.setVisible(false);
            requestDepublishAction.setVisible(false);
            requestScheduleDepublishAction.setVisible(false);
            schedulePublishAction.setVisible(false);
            scheduleDepublishAction.setVisible(false);
        }
    }
}
