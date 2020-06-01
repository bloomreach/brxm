/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.NT_DIRECTORY;
import static org.hippoecm.repository.HippoStdNodeType.NT_FOLDER;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

/**
 * Workflow plugin which adds non-application programmer accessible
 * functionality to the CMS, allowing all documents in a folder or directory
 * and recursively below the folder or directory to be published or
 * unpublished.
 *
 * This class is NOT part of any API provided and should not be extended by
 * other projects despite having public signature.
 *
 * Errors emanating from bad configuration, unable to query or mid-air
 * conflicts when gathering documents are reported as errors in the log, while
 * non-fatal errors that are caused by documents which can currently not be
 * (un)published are logged as warnings.  This even though they are not really
 * serious, but you do want to keep track of them.
 */
public class ExtendedFolderWorkflowPlugin extends RenderPlugin {

    private static Logger log = LoggerFactory.getLogger(ExtendedFolderWorkflowPlugin.class);

    private static final String WORKFLOW_CATEGORY = "default";

    private String name;
    private int processed;
    private Set<String> documents;

    public ExtendedFolderWorkflowPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("publishAll", new StringResourceModel("publish-all-label", this), context, getModel()) {

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.CHECK_CIRCLE);
            }

            @Override
            protected Dialog createRequestDialog() {
                try {
                    name = ((HippoNode) getModel().getNode()).getDisplayName();
                } catch(RepositoryException ex) {
                    name = "";
                }
                documents = new HashSet<>();
                return new ConfirmBulkWorkflowDialog(this,
                        new StringResourceModel("publish-all-title", ExtendedFolderWorkflowPlugin.this),
                        new StringResourceModel("publish-all-text", ExtendedFolderWorkflowPlugin.this),
                        new StringResourceModel("publish-all-subtext", ExtendedFolderWorkflowPlugin.this),
                        new PropertyModel(ExtendedFolderWorkflowPlugin.this, "name"),
                        documents, "publish");
            }

            @Override
            protected void execute(WorkflowDescriptorModel model) throws RepositoryException {
                bulkExecuteDocumentWorkflow("publish");
            }
        });

        add(new StdWorkflow("depublishAll", new StringResourceModel("depublish-all-label", this), context, getModel()) {

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.MINUS_CIRCLE);
            }

            @Override
            protected Dialog createRequestDialog() {
                try {
                    name = ((HippoNode)((WorkflowDescriptorModel)getDefaultModel()).getNode()).getDisplayName();
                } catch(RepositoryException ex) {
                    name = "";
                }
                documents = new HashSet<>();
                return new ConfirmBulkWorkflowDialog(this,
                        new StringResourceModel("depublish-all-title", ExtendedFolderWorkflowPlugin.this),
                        new StringResourceModel("depublish-all-text", ExtendedFolderWorkflowPlugin.this),
                        new StringResourceModel("depublish-all-subtext", ExtendedFolderWorkflowPlugin.this),
                        new PropertyModel(ExtendedFolderWorkflowPlugin.this, "name"),
                        documents, "depublish");
            }

            @Override
            protected void execute(WorkflowDescriptorModel model) throws Exception {
                bulkExecuteDocumentWorkflow("depublish");
            }
        });
    }

    private void bulkExecuteDocumentWorkflow(final String action) throws RepositoryException {
        Session session = UserSession.get().getJcrSession();
        WorkflowManager wfMgr = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        for (String uuid : documents) {
            try {
                Node handle = session.getNodeByIdentifier(uuid);
                if (handle.isNodeType(NT_HANDLE)) {
                    Workflow workflow = wfMgr.getWorkflow(WORKFLOW_CATEGORY, handle);
                    if (workflow instanceof DocumentWorkflow) {
                        DocumentWorkflow docWorkflow = (DocumentWorkflow) workflow;
                        switch (action) {
                            case "publish": docWorkflow.publish(); break;
                            case "depublish" : docWorkflow.depublish(); break;
                        }
                        ++processed;
                        log.info("executed action {} on document {} ({})", action, handle.getPath(), uuid);
                    }
                }
            } catch (RepositoryException | RemoteException | WorkflowException e) {
                log.warn("Execution of action {} on {} failed: {}", action, uuid, e);
            }
            session.refresh(true);
        }
    }

    @Override
    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) super.getModel();
    }

    public class ConfirmBulkWorkflowDialog extends WorkflowDialog<WorkflowDescriptor> {

        private Label affectedComponent;
        private final String workflowAction;

        public ConfirmBulkWorkflowDialog(IWorkflowInvoker invoker, IModel<String> dialogTitle, IModel dialogText,
                                         IModel dialogSubText, IModel folderName, Set<String> documents, String workflowAction) {
            super(invoker, ExtendedFolderWorkflowPlugin.this.getModel());

            setTitle(dialogTitle);
            setSize(DialogConstants.MEDIUM_AUTO);

            this.workflowAction = workflowAction;
            try {
                Node folder = getWorkflowDescriptorModel().getNode();
                if (folder != null) {
                    loadPublishableDocuments(folder, documents);
                } else {
                    error("Error preparing to (de)publish all documents");
                }
            } catch(RepositoryException | RemoteException | WorkflowException ex) {
                log.error("Error preparing to (de)publish all documents", ex);
                error("Error preparing to (de)publish all documents");
            }

            Label textComponent = new Label("text");
            textComponent.setDefaultModel(dialogText);
            add(textComponent);

            add(new Label("counttext", dialogSubText));

            Label countComponent = new Label("count");
            countComponent.setDefaultModel(new Model<>(Integer.toString(documents.size())));
            add(countComponent);

            Label locationComponent = new Label("location");
            locationComponent.setDefaultModel(new Model<>((String) folderName.getObject()));
            add(locationComponent);

            affectedComponent = new Label("affected");
            affectedComponent.setVisible(false);
            add(affectedComponent);

            setOkEnabled(!documents.isEmpty());
        }

        private void loadPublishableDocuments(final Node folder, final Set<String> documents) throws RepositoryException, WorkflowException, RemoteException {
            for (Node child : new NodeIterable(folder.getNodes())) {
                if (child.isNodeType(NT_FOLDER) || child.isNodeType(NT_DIRECTORY)) {
                    loadPublishableDocuments(child, documents);
                } else if (child.isNodeType(NT_HANDLE)) {
                    WorkflowManager workflowManager = ((HippoWorkspace) folder.getSession().getWorkspace()).getWorkflowManager();
                    Workflow workflow = workflowManager.getWorkflow(WORKFLOW_CATEGORY, child);
                    if (workflow != null) {
                        Serializable hint = workflow.hints().get(workflowAction);
                        if (hint instanceof Boolean && (Boolean) hint) {
                            documents.add(child.getIdentifier());
                        }
                    }
                }
            }
        }

        public WorkflowDescriptorModel getWorkflowDescriptorModel() {
            return (WorkflowDescriptorModel) super.getModel();
        }

        @Override
        protected void handleSubmit() {
            setOkVisible(false);
            setCancelLabel(new StringResourceModel("done-label", this));
            onOk();
            affectedComponent.setDefaultModel(new Model<>(Integer.toString(processed)));
            affectedComponent.setVisible(true);
            RequestCycle.get().find(AjaxRequestTarget.class).add(this);
        }
    }
}
