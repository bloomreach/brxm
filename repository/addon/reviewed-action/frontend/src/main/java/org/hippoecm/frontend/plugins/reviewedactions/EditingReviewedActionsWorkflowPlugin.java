/*
 *  Copyright 2008 Hippo.
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

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.Session;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.OnCloseDialog;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.IValidateService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingReviewedActionsWorkflowPlugin extends CompatibilityWorkflowPlugin implements IValidateService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(EditingReviewedActionsWorkflowPlugin.class);

    private transient boolean validated = false;
    private transient boolean isvalid = true;
    private transient boolean closing = false;

    public EditingReviewedActionsWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        if (config.getString(IValidateService.VALIDATE_ID) != null) {
            context.registerService(this, config.getString(IValidateService.VALIDATE_ID));
        } else {
            log.info("No validator id {} defined", IValidateService.VALIDATE_ID);
        }

        final CompatibilityWorkflowPlugin plugin = this;
        final IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);
        context.registerService(new IEditorFilter() {
            private static final long serialVersionUID = 1L;

            public void postClose(Object object) {
            }

            public Object preClose() {
                if (!closing) {
                    try {
                        OnCloseDialog.Actions actions = new OnCloseDialog.Actions() {
                            public void revert() {
                                try {
                                    UserSession session = (UserSession) org.apache.wicket.Session.get();
                                    WorkflowDescriptor descriptor = (WorkflowDescriptor) plugin.getModelObject();
                                    WorkflowManager manager = session.getWorkflowManager();
                                    Node handleNode = ((WorkflowDescriptorModel) plugin.getModel()).getNode();
                                    if (handleNode.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                                        handleNode = handleNode.getParent();
                                    }
                                    handleNode.refresh(false);
                                    handleNode.getSession().refresh(true);
                                    ((EditableWorkflow) manager.getWorkflow(descriptor)).disposeEditableInstance();
                                    session.getJcrSession().refresh(true);
                                } catch (RepositoryException ex) {
                                    log.error("failure while reverting", ex);
                                } catch (WorkflowException ex) {
                                    log.error("failure while reverting", ex);
                                } catch (RemoteException ex) {
                                    log.error("failure while reverting", ex);
                                }
                            }

                            public void save() {
                                try {
                                    UserSession userSession = (UserSession) org.apache.wicket.Session.get();
                                    WorkflowDescriptor descriptor = (WorkflowDescriptor) plugin.getModelObject();
                                    WorkflowManager manager = userSession.getWorkflowManager();
                                    userSession.getJcrSession().save();
                                    userSession.getJcrSession().refresh(true);
                                    ((EditableWorkflow) manager.getWorkflow(descriptor)).disposeEditableInstance();
                                    userSession.getJcrSession().refresh(false);
                                } catch (RepositoryException ex) {
                                    log.error("failure while reverting", ex);
                                } catch (WorkflowException ex) {
                                    log.error("failure while reverting", ex);
                                } catch (RemoteException ex) {
                                    log.error("failure while reverting", ex);
                                }
                            }

                            public void close() {
                                IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);
                                try {
                                    // prevent reentrancy
                                    closing = true;
                                    editor.close();
                                } catch (EditorException ex) {
                                    log.error(ex.getMessage());
                                } finally {
                                    closing = false;
                                }
                            }
                        };

                        Node node = ((WorkflowDescriptorModel) getModel()).getNode();
                        boolean dirty = node.isModified();
                        if (!dirty) {
                            HippoSession session = ((HippoSession) node.getSession());
                            NodeIterator nodes = session.pendingChanges(node, "nt:base", true);
                            if (nodes.hasNext()) {
                                dirty = true;
                            }
                        }
                        if (dirty) {
                            IDialogService dialogService = context.getService(IDialogService.class.getName(),
                                    IDialogService.class);
                            dialogService.show(new OnCloseDialog(actions, new JcrNodeModel(node), editor));
                        } else {
                            actions.revert();
                            return new Object();
                        }
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                        return new Object();
                    }
                    return null;
                } else {
                    return new Object();
                }
            }

        }, context.getReference(editor).getServiceId());

        add(new WorkflowAction("save", new StringResourceModel("save", this, null, "Save").getString(), null) {
            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.commitEditableInstance();

                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                info(new StringResourceModel("saved", EditingReviewedActionsWorkflowPlugin.this, null,
                        new Object[] { df.format(new Date()) }).getString());

                UserSession session = (UserSession) Session.get();
                session.getJcrSession().refresh(false);

                // get new instance of the workflow, previous one may have invalidated itself
                EditingReviewedActionsWorkflowPlugin.this.getModel().detach();
                WorkflowDescriptor descriptor = (WorkflowDescriptor) (EditingReviewedActionsWorkflowPlugin.this
                        .getModel().getObject());
                session.getJcrSession().refresh(true);
                WorkflowManager manager = session.getWorkflowManager();
                workflow = (BasicReviewedActionsWorkflow) manager.getWorkflow(descriptor);

                Document draft = workflow.obtainEditableInstance();
                IModelReference ref = context.getService(config.getString("model.id"), IModelReference.class);
                ref.setModel(new JcrNodeModel(session.getJcrSession().getNodeByUUID(draft.getIdentity())));
                return null;
            }
        });

        add(new WorkflowAction("done", new StringResourceModel("done", this, null, "Done").getString(), null) {
            @Override
            public String execute(Workflow wf) throws Exception {
                IPluginConfig config = getPluginConfig();
                IBrowseService browser = context.getService(config.getString("browser.id"), IBrowseService.class);
                IRenderService browserRenderer = context.getService(config.getString("browser.id"),
                        IRenderService.class);
                IEditor editor = context.getService(config.getString(IEditorManager.EDITOR_ID), IEditor.class);

                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.commitEditableInstance();
                ((UserSession) Session.get()).getJcrSession().refresh(true);

                browser.browse(new JcrNodeModel(((WorkflowDescriptorModel) EditingReviewedActionsWorkflowPlugin.this
                        .getModel()).getNode()));

                closing = true;
                editor.close();
                browserRenderer.focus(null);
                return null;
            }
        });
    }

    public boolean hasError() {
        if (!validated) {
            validate();
        }
        return !isvalid;
    }

    public void validate() {
        isvalid = true;
        try {
            Node handle = ((WorkflowDescriptorModel) getModel()).getNode();
            String currentUser = handle.getSession().getUserID();
            NodeIterator variants = handle.getNodes(handle.getName());
            Node toBeValidated = null;
            while (variants.hasNext()) {
                Node variant = variants.nextNode();
                if (variant.hasProperty("hippostd:state")
                        && variant.getProperty("hippostd:state").getString().equals("draft")) {
                    if (variant.hasProperty("hippostd:holder")
                            && variant.getProperty("hippostd:holder").getString().equals(currentUser)) {
                        toBeValidated = variant;
                        break;
                    }
                }
            }
            if (toBeValidated != null) {
                PropertyIterator properties = toBeValidated.getProperties();
                while (properties.hasNext()) {
                    PropertyDefinition propertyDefinition = properties.nextProperty().getDefinition();
                    if (propertyDefinition.isMandatory()) {
                        String propName = propertyDefinition.getName();
                        if (toBeValidated.hasProperty(propName)) {
                            Property mandatory = toBeValidated.getProperty(propName);
                            if (mandatory.getDefinition().isMultiple()) {
                                if (mandatory.getLengths().length == 0) {
                                    isvalid = false;
                                    error("Mandatory field " + propName + " has no value.");
                                    break;
                                } else {
                                    for (Value value : mandatory.getValues()) {
                                        if (value.getString() == null || value.getString().equals("")) {
                                            isvalid = false;
                                            error("Mandatory field " + propName + " has no value.");
                                            break;
                                        }
                                    }
                                }
                            } else {
                                Value value = mandatory.getValue();
                                if (value == null || value.getString() == null || value.getString().equals("")) {
                                    isvalid = false;
                                    error("Mandatory field " + propName + " has no value.");
                                    break;
                                }
                            }
                        } else {
                            isvalid = false;
                            error("Mandatory field " + propName + " has no value.");
                            break;
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            error("Problem while validating: " + ex.getMessage());
            isvalid = false;
        }
        validated = true;
    }
}
