/*
 *  Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.ConfirmDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.buttons.ButtonStyle;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

public abstract class AbstractPreviewWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    /** Workflow action that edits the unpublished variant of a document. */
    private final StdWorkflow editAction;
    /** Workflow action that edits the current draft if it has been saved as draft. */
    private final StdWorkflow editDraftAction;
    private final Map<String, Serializable> info;

    @SuppressWarnings({"unused", "FieldCanBeLocal"}) // used by a PropertyModel
    private String inUseBy;

    protected AbstractPreviewWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        info = getHints();
        inUseBy = getHint("inUseBy");
        add(new StdWorkflow("infoEdit", "infoEdit") {

            /**
             * Gets whether this component and any children are visible.
             * <p>
             * WARNING: this method can be called multiple times during a request. If you override this method, it is a good
             * idea to keep it cheap in terms of processing. Alternatively, you can call {@link #setVisible(boolean)}.
             * <p>
             *
             * @return True if component and any children are visible
             */
            @Override
            public boolean isVisible() {
                return StringUtils.isNotEmpty(inUseBy);
            }

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel getTitle() {
                return new StringResourceModel("in-use-by", this).setModel(null).setParameters(
                        new PropertyModel(AbstractPreviewWorkflowPlugin.this, "inUseBy"));
            }

            @Override
            protected void invoke() {
                // The infoEdit workflow only shows feedback based on the hints.
                // It does not show any dialog.
            }
        });


        editDraftAction = new StdWorkflow("editDraftAction", new StringResourceModel("edit-draft-label", this), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            public String getCssClass() {
                return ButtonStyle.SECONDARY.getCssClass();
            }


            @Override
            protected String execute(final Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                Document docRef = workflow.editDraft();
                return openEditor(docRef);
            }

        };
        final StringResourceModel editDraftResourceModel =
                new StringResourceModel("edit-label", this);
        final boolean isTransferable = isActionAllowed(info, "editDraft");

        editAction = new StdWorkflow("edit", editDraftResourceModel, getPluginContext(), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            public String getCssClass() {
                return isTransferable ? super.getCssClass() : ButtonStyle.SECONDARY.getCssClass();
            }

            @Override
            protected IModel<String> getTooltip() {
                final StringResourceModel model = new StringResourceModel("edit-discard-changes-hint", AbstractPreviewWorkflowPlugin.this);
                return isTransferable ? model : super.getTooltip();
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                ConfirmDialog confirmationDialog = new ConfirmDialog(new StringResourceModel("edit-discard-changes-confirmation-title", AbstractPreviewWorkflowPlugin.this)
                        , new StringResourceModel("edit-discard-changes-confirmation-body", AbstractPreviewWorkflowPlugin.this)) {
                    @Override
                    public void invokeWorkflow() throws Exception {
                        editAction.invokeWorkflow();
                    }
                };
                return isTransferable ? confirmationDialog : super.createRequestDialog();
            }

            @Override
            protected String execute(final Workflow wf) throws Exception {
                DocumentWorkflow workflow = (DocumentWorkflow) wf;
                String branchId = getBranchIdModel().getBranchId();
                Document docRef = workflow.obtainEditableInstance(branchId);
                return openEditor(docRef);
            }

        };

        add(editDraftAction);
        add(editAction);
        hideInvalidActions();
    }

    @SuppressWarnings("unused")  // used by a PropertyModel
    public String getStateSummary() {
        try {
            WorkflowDescriptorModel wdm = getModel();
            Node handleNode = wdm.getNode();
            for (Node child : new NodeIterable(handleNode.getNodes())) {
                if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY)) {
                    return child.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString();
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Unable to ascertain state summary", ex);
        }
        return StringUtils.EMPTY;
    }

    private void hideInvalidActions() {
        hideIfNotAllowed(info, "obtainEditableInstance", editAction);
        hideIfNotAllowed(info, "editDraft", editDraftAction);
    }

    protected final String getHint(final String key) {
        final Serializable serializable = info.get(key);
        if (serializable instanceof String) {
            return (String) serializable;
        }
        return StringUtils.EMPTY;
    }

}
