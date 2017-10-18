/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Session;

import org.apache.wicket.Component;
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
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standardworkflow.ContextPayloadProvider;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditmodelWorkflowPlugin extends CompatibilityWorkflowPlugin {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EditmodelWorkflowPlugin.class);
    private WorkflowAction editAction;

    public EditmodelWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(editAction = new WorkflowAction("edit", new StringResourceModel("edit", this, null)) {

            @Override
            public String getSubMenu() {
                return "top";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.PENCIL_SQUARE);
            }

            @Override
            public String execute(Workflow workflow) throws Exception {
                if (workflow == null) {
                    log.error("No workflow defined on model for selected node");
                    return null;
                }

                final String path = ((EditmodelWorkflow) workflow).edit();
                if (path == null) {
                    log.error("No model found to edit");
                    return null;
                }

                final String serviceId = config.getString(IEditorManager.EDITOR_ID);
                final IEditorManager editorMgr = context.getService(serviceId, IEditorManager.class);
                if (editorMgr == null) {
                    log.warn("No view service found for id '{}'", serviceId);
                    return null;
                }

                final Session session = UserSession.get().getJcrSession();
                final Node node;
                try {
                    node = JcrUtils.getNodeIfExists(path, session);
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                    return null;
                }

                if (node == null) {
                    log.error("No model found at path '{}'", path);
                    return null;
                }

                final JcrNodeModel nodeModel = new JcrNodeModel(node);
                final IEditor editor = editorMgr.getEditor(nodeModel);
                if (editor == null) {
                    editorMgr.openEditor(nodeModel);
                } else {
                    editor.setMode(IEditor.Mode.EDIT);
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
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.FILES);
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
                Map<String, Serializable> info = workflow.hints(ContextPayloadProvider.get());
                if (info.containsKey("edit")) {
                    Object editObject = info.get("edit");
                    if (editObject instanceof Boolean) {
                        editAction.setVisible((Boolean) editObject);
                    }
                }
            }
        } catch (RepositoryException | WorkflowException | RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public class CopyModelDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {

        private String name;

        public CopyModelDialog(CompatibilityWorkflowPlugin.WorkflowAction action) {
            action.super();
            WorkflowDescriptorModel workflowModel = (WorkflowDescriptorModel) EditmodelWorkflowPlugin.this.getDefaultModel();
            PropertyModel<String> model = new PropertyModel<>(action, "name");
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

        private Exception exception;

        ExceptionError(Exception e) {
            this.exception = e;
        }

        public String getErrorMessage(IErrorMessageSource messageSource) {
            return exception.getLocalizedMessage();
        }

    }
}
