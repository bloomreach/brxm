/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.buttons.ButtonStyle;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.skin.CmsIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.Workflow;

public class EditingWorkflowPlugin extends AbstractDocumentWorkflowPlugin {

    public static final String SERVICE_EDIT = "service.edit";
    private final StdWorkflow saveDraftAction;

    public EditingWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("save", new StringResourceModel("save", this).setDefaultValue("Save"), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.FLOPPY);
            }

            @Override
            public String getCssClass() {
                return ButtonStyle.SECONDARY.getCssClass();
            }

            @Override
            protected IModel<String> getTooltip() {
                return new StringResourceModel("save-hint", this);
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                final IEditorManager editorMgr = context.getService(SERVICE_EDIT, IEditorManager.class);
                IEditor<Node> editor = editorMgr.getEditor(new JcrNodeModel(getModel().getNode()));
                editor.save();
                return null;
            }
        });


        add(new StdWorkflow("done", new StringResourceModel("done", this).setDefaultValue("Done"), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.inline(id, CmsIcon.FLOPPY_TIMES_CIRCLE);
            }

            @Override
            public String getCssClass() {
                return ButtonStyle.PRIMARY.getCssClass();
            }

            @Override
            protected IModel<String> getTooltip() {
                return new StringResourceModel("done-hint", this);
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            public String execute(Workflow wf) throws Exception {
                final IEditorManager editorMgr = context.getService(SERVICE_EDIT, IEditorManager.class);
                IEditor<Node> editor = editorMgr.getEditor(new JcrNodeModel(getModel().getNode()));
                editor.done();
                return null;
            }
        });

        saveDraftAction = new StdWorkflow("saveDraft",
                new StringResourceModel("save-draft", this).setDefaultValue("Keep draft"), getModel()) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.FLOPPY);
            }

            @Override
            protected IModel<String> getTooltip() {
                return new StringResourceModel("save-draft-hint", this);
            }

            @Override
            public String getCssClass() {
                return ButtonStyle.PRIMARY.getCssClass();
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                final IEditorManager editorMgr = context.getService(SERVICE_EDIT, IEditorManager.class);
                IEditor<Node> editor = editorMgr.getEditor(new JcrNodeModel(getModel().getNode()));
                editor.saveDraft();
                return null;
            }
        };
        add(saveDraftAction);

        add(new StdWorkflow("cancel", new StringResourceModel("cancel", this).setDefaultValue("Cancel"), context, getModel()) {
            final StdWorkflow cancelAction = this;

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.TIMES_CIRCLE);
            }

            @Override
            protected IModel<String> getTooltip() {
                return new StringResourceModel("cancel-hint", this);
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            protected boolean invokeOnFormError() {
                return true;
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                try {
                    final IEditorManager editorMgr = context.getService(SERVICE_EDIT, IEditorManager.class);
                    IEditor<Node> editor = editorMgr.getEditor(new JcrNodeModel(getModel().getNode()));

                    if (editor.isModified() || !editor.isValid()) {
                        return new CancelDialog(
                                new StringResourceModel("cancel-dialog-title", EditingWorkflowPlugin.this),
                                new StringResourceModel("cancel-dialog-message", EditingWorkflowPlugin.this)
                                        .setParameters(getDocumentDisplayName()),
                                new StringResourceModel("cancel-dialog-ok-button", EditingWorkflowPlugin.this),
                                cancelAction);
                    }

                } catch (RepositoryException e) {
                    log.error("Could not retrieve workflow document", e);
                } catch (EditorException e) {
                    log.error("Could not retrieve document editor", e);
                }
                return null;
            }

            private String getDocumentDisplayName() {
                try {
                    final HippoNode node = (HippoNode) getModel().getNode();
                    return node.getDisplayName();
                } catch (RepositoryException e) {
                    log.error("Could not retrieve workflow document", e);
                }
                return StringUtils.EMPTY;
            }

            @Override
            public String execute(Workflow wf) throws Exception {
                final IEditorManager editorMgr = context.getService(SERVICE_EDIT, IEditorManager.class);
                IEditor<Node> editor = editorMgr.getEditor(new JcrNodeModel(getModel().getNode()));
                editor.discard();
                return null;
            }
        });

        hideOrDisable(getHints(),"saveDraft",saveDraftAction);
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }

}
