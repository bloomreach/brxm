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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.validation.IErrorMessageSource;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.editor.NamespaceValidator;
import org.hippoecm.editor.repository.EditmodelWorkflow;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditmodelWorkflowPlugin extends CompatibilityWorkflowPlugin {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EditmodelWorkflowPlugin.class);
    private WorkflowAction editAction;

    public EditmodelWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(editAction = new WorkflowAction("edit", new StringResourceModel("edit", this, null)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            public String execute(Workflow workflow) throws Exception {
                EditmodelWorkflow emWorkflow = (EditmodelWorkflow) workflow;
                if (emWorkflow != null) {
                    String path = emWorkflow.edit();
                    try {
                        Node node = UserSession.get().getJcrSession().getRootNode().getNode(path.substring(1));
                        JcrItemModel itemModel = new JcrItemModel(node);
                        if (path != null) {
                            IEditorManager editorMgr = context.getService(config.getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                            if (editorMgr != null) {
                                JcrNodeModel nodeModel = new JcrNodeModel(itemModel);
                                IEditor editor = editorMgr.getEditor(nodeModel);
                                if (editor == null) {
                                    editorMgr.openEditor(nodeModel);
                                } else {
                                    editor.setMode(IEditor.Mode.EDIT);
                                }
                            } else {
                                log.warn("No view service found");
                            }
                        } else {
                            log.error("no model found to edit");
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                } else {
                    log.error("no workflow defined on model for selected node");
                }
                return null;
            }
        });

        add(new WorkflowAction("copy", new StringResourceModel("copy", this, null)) {
            private static final long serialVersionUID = 1L;

            String name;

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Dialog createRequestDialog() {
                return new CopyModelDialog(this);
            }

            @Override
            protected String execute(Workflow wf) {
                try {
                    EditmodelWorkflow workflow = (EditmodelWorkflow) wf;
                    if (workflow != null) {
                        String path = workflow.copy(name);
                        UserSession.get().getJcrSession().refresh(true);

                        JcrNodeModel nodeModel = new JcrNodeModel(path);
                        if (path != null) {
                            IPluginContext context = EditmodelWorkflowPlugin.this.getPluginContext();
                            IPluginConfig config = EditmodelWorkflowPlugin.this.getPluginConfig();

                            IEditorManager editService = context.getService(config.getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                            IEditor editor = editService.openEditor(nodeModel);
                            IRenderService renderer = context.getService(context.getReference(editor).getServiceId(), IRenderService.class);
                            if (renderer != null) {
                                renderer.focus(null);
                            }
                        } else {
                            log.error("no model found to edit");
                        }
                    } else {
                        log.error("no workflow defined on model for selected node");
                    }
                } catch (WorkflowException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                } catch (ServiceException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                } catch (RemoteException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                } catch (RepositoryException ex) {
                    return ex.getClass().getName() + ": " + ex.getMessage();
                }
                return null;
            }
        });

        try {
            WorkflowManager manager = UserSession.get().getWorkflowManager();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
            if (workflowDescriptor != null) {
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();
                if (info.containsKey("edit") && info.get("edit") instanceof Boolean && !((Boolean) info.get("edit")).booleanValue()) {
                    editAction.setVisible(false);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        } catch (WorkflowException ex) {
            log.error(ex.getMessage(), ex);
        } catch (RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public class CopyModelDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {
        private static final long serialVersionUID = 1L;

        private String name;

        public CopyModelDialog(CompatibilityWorkflowPlugin.WorkflowAction action) {
            action.super();
            WorkflowDescriptorModel workflowModel = (WorkflowDescriptorModel) EditmodelWorkflowPlugin.this.getDefaultModel();
            PropertyModel model = new PropertyModel(action, "name");
            try {
                model.setObject(name = workflowModel.getNode().getName());
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            TextFieldWidget widget = new TextFieldWidget("name", model);
            ((FormComponent) widget.get("widget")).add(new IValidator() {
                private static final long serialVersionUID = 1L;

                public void validate(IValidatable validatable) {
                    try {
                        NamespaceValidator.checkName((String) validatable.getValue());
                    } catch (Exception e) {
                        validatable.error(new ExceptionError(e));
                    }
                }

            }).setRequired(true);
            add(widget);
            setFocus(widget);
        }

        @Override
        public IModel getTitle() {
            return new StringResourceModel("copy-model", this, null, new PropertyModel(this, "name"));
        }

        @Override
        public IValueMap getProperties() {
            return DialogConstants.SMALL;
        }
    }

    private static class ExceptionError implements IValidationError, IClusterable {
        private static final long serialVersionUID = 1L;

        private Exception exception;

        ExceptionError(Exception e) {
            this.exception = e;
        }

        public String getErrorMessage(IErrorMessageSource messageSource) {
            return exception.getLocalizedMessage();
        }

    }
}
