/*
 *  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.DepublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.ScheduleDepublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.SchedulePublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.UnpublishedReferencesDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;

public class PublicationWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    // Wicket component ID's
    private static final String DEPUBLICATION_ID = "DEPUB";
    private static final String SCHEDULE_DEPUBLICATION_ID = "SCHED_DEPUB";
    private static final String REQUEST_DEPUBLICATION_ID = "REQ_DEPUB";
    private static final String REQUEST_SCHEDULE_DEPUBLICATION_ID = "REQ_SCHED_DEPUB";

    private static final String PUBLICATION_ID = "PUB";
    private static final String SCHEDULE_PUBLICATION_ID = "SCHED_PUB";
    private static final String REQUEST_PUBLICATION_ID = "REQ_PUB";
    private static final String REQUEST_SCHEDULE_PUBLICATION_ID = "REQ_SCHED_PUB";

    // Action keys
    private static final String PUBLISH = "publish";
    private static final String DEPUBLISH = "depublish";
    private static final String REQUEST_PUBLICATION = "requestPublication";
    private static final String REQUEST_DEPUBLICATION = "requestDepublication";

    private StdDocumentWorkflow publishAction;

    public PublicationWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final List<StdDocumentWorkflow> workflows = getStdWorkflows(context);
        workflows.forEach(this::add);

        final Map<String, StdWorkflow> workflowMap = workflows.stream()
                .collect(toMap(StdWorkflow::getId, Function.identity()));
        setVisibility(workflowMap);
    }

    protected List<StdDocumentWorkflow> getStdWorkflows(final IPluginContext context) {

        final List<StdDocumentWorkflow> workflows = new ArrayList<>();

        workflows.add(new StdDocumentWorkflow(DEPUBLICATION_ID, new StringResourceModel("depublish-label", this),
                context, getModel(), Icon.MINUS_CIRCLE) {

            @Override
            protected Dialog createRequestDialog() {
                final IModel docName = getDocumentName();
                final IModel<String> title = new StringResourceModel("depublish-title", PublicationWorkflowPlugin.this)
                        .setParameters(docName);
                final IModel<String> message = new StringResourceModel("depublish-message", PublicationWorkflowPlugin.this)
                        .setParameters(docName);
                return new DepublishDialog(this, getModel(), title, message, getEditorManager());
            }

            @Override
            protected String execute(final DocumentWorkflow workflow) throws Exception {
                workflow.depublish();
                return null;
            }
        });

        workflows.add(new StdDocumentWorkflow(REQUEST_DEPUBLICATION_ID,
                new StringResourceModel("request-depublication-label", this),
                context, getModel(), Icon.MINUS_CIRCLE) {

            @Override
            protected Dialog createRequestDialog() {
                final IModel docName = getDocumentName();
                final IModel<String> title = new StringResourceModel("depublish-title", PublicationWorkflowPlugin.this)
                        .setParameters(docName);
                final IModel<String> message = new StringResourceModel("depublish-message", PublicationWorkflowPlugin.this)
                        .setParameters(docName);
                return new DepublishDialog(this, getModel(), title, message, getEditorManager());
            }

            @Override
            protected String execute(final DocumentWorkflow workflow) throws Exception {
                workflow.requestDepublication();
                return null;
            }
        });

        workflows.add(new StdDocumentWorkflow(SCHEDULE_DEPUBLICATION_ID,
                new StringResourceModel("schedule-depublish-label", this),
                context, getModel(), Icon.MINUS_CIRCLE_CLOCK) {

            public Date date = new Date();

            @Override
            protected Dialog createRequestDialog() {
                final WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                try {
                    final IModel<String> titleModel = new StringResourceModel("schedule-depublish-title", PublicationWorkflowPlugin.this)
                            .setParameters(getDocumentName());

                    return new ScheduleDepublishDialog(this, new JcrNodeModel(wdm.getNode()),
                            PropertyModel.of(this, "date"), titleModel, getEditorManager());
                } catch (final RepositoryException e) {
                    log.warn("could not retrieve node for scheduling depublish", e);
                }
                return null;
            }

            @Override
            protected String execute(final DocumentWorkflow workflow) throws Exception {
                if (date != null) {
                    workflow.depublish(date);
                } else {
                    workflow.depublish();
                }
                return null;
            }
        });

        workflows.add(new StdDocumentWorkflow(REQUEST_SCHEDULE_DEPUBLICATION_ID,
                new StringResourceModel("schedule-request-depublish-label", this),
                context, getModel(), Icon.MINUS_CIRCLE_CLOCK) {

            public Date date = new Date();

            @Override
            protected Dialog createRequestDialog() {
                final WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                try {
                    final IModel<String> titleModel = new StringResourceModel("schedule-depublish-title", PublicationWorkflowPlugin.this)
                            .setParameters(getDocumentName());
                    return new ScheduleDepublishDialog(this, new JcrNodeModel(wdm.getNode()),
                            PropertyModel.of(this, "date"), titleModel, getEditorManager());
                } catch (final RepositoryException e) {
                    log.warn("could not retrieve node for scheduling depublish", e);
                }
                return null;
            }

            @Override
            protected String execute(final DocumentWorkflow workflow) throws Exception {
                if (date != null) {
                    workflow.requestDepublication(date);
                } else {
                    workflow.requestDepublication();
                }
                return null;
            }
        });

        workflows.add(publishAction = new StdDocumentWorkflow(PUBLICATION_ID,
                new StringResourceModel("publish-label", this),
                context, getModel(), Icon.CHECK_CIRCLE) {

            @Override
            protected Dialog createRequestDialog() {
                try {
                    final Node handle = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    final Node unpublished = getVariant(handle, UNPUBLISHED);
                    final Session jcrSession = UserSession.get().getJcrSession();
                    final Map<String, Node> referencesToUnpublishedDocuments = WorkflowUtils.getReferencesToUnpublishedDocuments(
                            unpublished, jcrSession);

                    if (!referencesToUnpublishedDocuments.isEmpty()) {
                        return new UnpublishedReferencesDialog(publishAction,
                                new UnpublishedReferenceNodeProvider(referencesToUnpublishedDocuments),
                                getEditorManager());
                    }
                } catch (final RepositoryException e) {
                    log.error(e.getMessage());
                }
                return null;
            }

            @Override
            protected String execute(final DocumentWorkflow workflow) throws Exception {
                workflow.publish();
                return null;
            }
        });
        workflows.add(new StdDocumentWorkflow(REQUEST_PUBLICATION_ID,
                new StringResourceModel("request-publication-label", this),
                context, getModel(), Icon.CHECK_CIRCLE) {

            @Override
            protected Dialog createRequestDialog() {
                try {
                    final Node handle = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    final Node unpublished = getVariant(handle, UNPUBLISHED);
                    final Session jcrSession = UserSession.get().getJcrSession();
                    final Map<String, Node> referencesToUnpublishedDocuments = WorkflowUtils.getReferencesToUnpublishedDocuments(
                            unpublished, jcrSession);

                    if (!referencesToUnpublishedDocuments.isEmpty()) {
                        final UnpublishedReferenceNodeProvider provider = new UnpublishedReferenceNodeProvider(referencesToUnpublishedDocuments);
                        return new UnpublishedReferencesDialog(this, provider, getEditorManager());
                    }
                } catch (final RepositoryException e) {
                    log.error(e.getMessage());
                }
                return null;
            }

            @Override
            protected String execute(final DocumentWorkflow workflow) throws Exception {
                workflow.requestPublication();
                return null;
            }
        });

        workflows.add(new StdDocumentWorkflow(SCHEDULE_PUBLICATION_ID,
                new StringResourceModel("schedule-publish-label", this),
                context, getModel(), Icon.CHECK_CIRCLE_CLOCK) {

            public Date date = new Date();

            @Override
            protected Dialog createRequestDialog() {
                final WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                try {
                    final Node unpublished = getVariant(wdm.getNode(), UNPUBLISHED);
                    final IModel<String> titleModel = new StringResourceModel("schedule-publish-title", PublicationWorkflowPlugin.this)
                            .setParameters(getDocumentName());

                    return new SchedulePublishDialog(this, new JcrNodeModel(unpublished),
                            PropertyModel.of(this, "date"), titleModel, getEditorManager());
                } catch (final RepositoryException ex) {
                    log.warn("could not retrieve node for scheduling publish", ex);
                }
                return null;
            }

            @Override
            protected String execute(final DocumentWorkflow workflow) throws Exception {
                if (date != null) {
                    workflow.publish(date);
                } else {
                    workflow.publish();
                }
                return null;
            }
        });

        workflows.add(new StdDocumentWorkflow(REQUEST_SCHEDULE_PUBLICATION_ID,
                new StringResourceModel("schedule-request-publish-label", this),
                context, getModel(), Icon.CHECK_CIRCLE_CLOCK) {

            public Date date = new Date();

            @Override
            protected Dialog createRequestDialog() {
                final WorkflowDescriptorModel wdm = getModel();
                try {
                    final Node unpublished = getVariant(wdm.getNode(), UNPUBLISHED);
                    final IModel<String> titleModel = new StringResourceModel("schedule-publish-title", PublicationWorkflowPlugin.this)
                            .setParameters(getDocumentName());

                    return new SchedulePublishDialog(this, new JcrNodeModel(unpublished),
                            PropertyModel.of(this, "date"), titleModel, getEditorManager());
                } catch (final RepositoryException ex) {
                    log.warn("could not retrieve node for scheduling publish", ex);
                }
                return null;
            }

            @Override
            protected String execute(final DocumentWorkflow workflow) throws Exception {
                if (date != null) {
                    workflow.requestPublication(date);
                } else {
                    workflow.requestPublication();
                }
                return null;
            }
        });
        return workflows;
    }

    protected Stream<String> getActionKeys() {
        return Stream.of(PUBLISH, DEPUBLISH, REQUEST_PUBLICATION, REQUEST_DEPUBLICATION);
    }

    protected Map<String, String> getActionToRequestActionMap() {
        final Map<String, String> requestKeyMap = new HashMap<>();
        requestKeyMap.put(PUBLISH, REQUEST_PUBLICATION);
        requestKeyMap.put(DEPUBLISH, REQUEST_DEPUBLICATION);
        return requestKeyMap;
    }

    protected Map<String, List<String>> getActionToComponentIdMap() {
        final Map<String, List<String>> idMap = new HashMap<>();
        idMap.put(PUBLISH, asList(REQUEST_PUBLICATION_ID, REQUEST_SCHEDULE_PUBLICATION_ID));
        idMap.put(DEPUBLISH, asList(REQUEST_DEPUBLICATION_ID, REQUEST_SCHEDULE_DEPUBLICATION_ID));
        return idMap;
    }

    protected Map<String, List<String>> getActionToHiddenComponentIdMap() {
        final Map<String, List<String>> idHideMap = new HashMap<>();
        idHideMap.put(PUBLISH, asList(PUBLICATION_ID, SCHEDULE_PUBLICATION_ID));
        idHideMap.put(DEPUBLISH, asList(DEPUBLICATION_ID, SCHEDULE_DEPUBLICATION_ID));
        return idHideMap;
    }

    private void setVisibility(final Map<String, StdWorkflow> workflowMap) {

        if (getActionKeys().anyMatch(key -> isActionAllowed(getHints(), key))) {

            final Map<String, List<String>> hiddenComponentIdMap = getActionToHiddenComponentIdMap();
            final Map<String, List<String>> componentIdMap = getActionToComponentIdMap();
            final Map<String, String> requestKeyMap = getActionToRequestActionMap();

            final Map<String, Serializable> info = getHints();
            for (final String action : requestKeyMap.keySet()) {
                hiddenComponentIdMap.get(action)
                        .forEach(id -> hideOrDisable(info, action, workflowMap.get(id)));
                if (!info.containsKey(action)) {
                    componentIdMap.get(action)
                            .forEach(id -> hideOrDisable(info, requestKeyMap.get(action), workflowMap.get(id)));
                } else {
                    componentIdMap.get(action)
                            .forEach(id -> workflowMap.get(id).setVisible(false));
                }
            }
        } else {
            workflowMap.values().forEach(wf -> wf.setVisible(false));
        }
    }

}
