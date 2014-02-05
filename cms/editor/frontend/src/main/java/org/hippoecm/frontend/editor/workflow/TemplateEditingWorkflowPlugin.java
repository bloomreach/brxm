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
package org.hippoecm.frontend.editor.workflow;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateEditingWorkflowPlugin extends CompatibilityWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    static protected Logger log = LoggerFactory.getLogger(TemplateEditingWorkflowPlugin.class);

    boolean isValid = true;

    public TemplateEditingWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        if (config.getString(IValidationService.VALIDATE_ID) != null) {
            context.registerService(this, config.getString(IValidationService.VALIDATE_ID));
        } else {
            log.warn("No validator id {} defined", IValidationService.VALIDATE_ID);
        }

        IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);
        context.registerService(new IEditorFilter() {
            private static final long serialVersionUID = 1L;

            public void postClose(Object object) {
                // nothing to do
            }

            public Object preClose() {
                try {
                    Node node = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    boolean dirty = node.isModified();
                    if (!dirty) {
                        HippoSession session = (HippoSession) node.getSession();
                        NodeIterator nodes = session.pendingChanges(node, "nt:base", true);
                        if (nodes.hasNext()) {
                            dirty = true;
                        }
                    }
                    validate();
                    if (dirty || !isValid()) {
                        IDialogService dialogService = context.getService(IDialogService.class.getName(),
                                IDialogService.class);
                        dialogService.show(new OnCloseDialog());
                    } else {
                        return new Object();
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                    return new Object();
                }
                return null;
            }

        }, context.getReference(editor).getServiceId());

        add(new WorkflowAction("save", new StringResourceModel("save", this, null)) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            protected String execute(Workflow workflow) throws Exception {
                return null;
            }
        });
        add(new WorkflowAction("done", new StringResourceModel("done", this, null)) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            public boolean isFormSubmitted() {
                return true;
            }

            @Override
            protected String execute(Workflow workflow) throws Exception {
                if (isValid()) {
                    IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);
                    editor.setMode(IEditor.Mode.VIEW);
                }
                return null;
            }
        });
    }

    boolean isValid() {
        return isValid;
    }

    void validate() {
        isValid = true;
        try {
            IPluginConfig config = getPluginConfig();
            if (config.getString(IValidationService.VALIDATE_ID) != null) {
                List<IValidationService> validators = getPluginContext().getServices(
                        config.getString(IValidationService.VALIDATE_ID), IValidationService.class);
                for (IValidationService validator : validators) {
                    validator.validate();
                    isValid = validator.getValidationResult().isValid();
                }
            }
        } catch (ValidationException e) {
            log.error("error validating template", e);
        }
    }

    void doSave() throws Exception {
        UserSession.get().getJcrSession().save();
    }

    void doRevert() throws Exception {
        WorkflowDescriptorModel model = (WorkflowDescriptorModel) TemplateEditingWorkflowPlugin.this.getDefaultModel();
        model.getNode().refresh(false);
    }

    void closeEditor() {
        IEditor editor = getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditor.class);
        try {
            editor.close();
        } catch (EditorException e) {
            log.error("Could not close editor", e);
        }
    }

    private class OnCloseDialog extends AbstractDialog {
        private static final long serialVersionUID = 1L;

        public OnCloseDialog() {

            setOkVisible(false);

            final Label exceptionLabel = new Label("exception", "");
            exceptionLabel.setOutputMarkupId(true);
            add(exceptionLabel);

            AjaxButton button = new AjaxButton(DialogConstants.BUTTON) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    try {
                        doRevert();
                        closeDialog();
                        closeEditor();
                    } catch (Exception ex) {
                        exceptionLabel.setDefaultModel(new Model(ex.getMessage()));
                        target.add(exceptionLabel);
                    }
                }
            };
            button.setModel(new ResourceModel("discard", "Discard"));
            addButton(button);

            button = new AjaxButton(DialogConstants.BUTTON) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isEnabled() {
                    return super.isValid() && TemplateEditingWorkflowPlugin.this.isValid();
                }

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    try {
                        doSave();
                        closeDialog();
                        closeEditor();
                    } catch (Exception ex) {
                        exceptionLabel.setDefaultModel(new Model(ex.getMessage()));
                        target.add(exceptionLabel);
                    }
                }
            };
            button.setModel(new ResourceModel("save", "Save"));
            addButton(button);
        }

        public IModel getTitle() {
            return new StringResourceModel("close-document", this, null, "Close {0}",
                        new PropertyModel(TemplateEditingWorkflowPlugin.this, "model.node.name"));
        }

    }

}
