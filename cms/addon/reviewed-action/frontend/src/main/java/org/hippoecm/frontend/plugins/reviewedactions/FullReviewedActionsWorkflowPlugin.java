/*
 *  Copyright 2009 Hippo.
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;

public class FullReviewedActionsWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(FullReviewedActionsWorkflowPlugin.class);

    private IModel caption = new StringResourceModel("unknown", this, null);
    private String stateSummary = "UNKNOWN";
    private boolean isLocked = false;
    private boolean pendingRequest = false;
    private Component locked;

    public FullReviewedActionsWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new WorkflowAction("edit", "edit", null) {
            protected String execute(Workflow workflow) throws Exception {
                FullReviewedActionsWorkflow wf = (FullReviewedActionsWorkflow)workflow;
                Document docRef = wf.obtainEditableInstance();
                Session session = ((UserSession)getSession()).getJcrSession();
                session.refresh(false);
                Node docNode = session.getNodeByUUID(docRef.getIdentity());
                IEditorManager editorMgr = getPluginContext().getService(getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    editorMgr.openEditor(new JcrNodeModel(docNode));
                } else {
                    return "No editor found to edit " + docNode.getPath();
                }
                return null;
            }
        });
        
        add(new WorkflowAction("delete", "delete", null) {
            protected String execute(Workflow workflow) throws Exception {
                FullReviewedActionsWorkflow wf = (FullReviewedActionsWorkflow)workflow;
                wf.delete();
                return null;
            }
        });

        add(new StdWorkflow("copy", "copy") {
            protected void invoke() {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(new AbstractDialog() {

                    public IModel getTitle() {
                        return new Model("Sure");
                    }
                });
            }
        });

        add(new StdWorkflow("move", "move") {
            protected void invoke() {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(new AbstractDialog() {
                    public IModel getTitle() {
                        return new Model("Sure");
                    }
                });
            }
        });

        add(new StdWorkflow("rename", "rename") {
            protected void invoke() {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(new AbstractDialog() {

                    public IModel getTitle() {
                        return new Model("Sure");
                    }
                });
            }
        });

        add(new StdWorkflow("publish", "publish") {
            protected void invoke() {
                try {
                    WorkflowDescriptor descriptor = (WorkflowDescriptor) FullReviewedActionsWorkflowPlugin.this.getModelObject();
                    Session session = ((UserSession) getSession()).getJcrSession();
                    session.refresh(true);
                    session.save();
                    WorkflowManager manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
                    FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) manager.getWorkflow(descriptor);
                    workflow.publish();
                    session.refresh(false);
                } catch(WorkflowException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                } catch(RemoteException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                } catch(RepositoryException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
        });

        add(new StdWorkflow("depublish", "depublish") {
            protected void invoke() {
                try {
                    WorkflowDescriptor descriptor = (WorkflowDescriptor) FullReviewedActionsWorkflowPlugin.this.getModelObject();
                    Session session = ((UserSession) getSession()).getJcrSession();
                    session.refresh(true);
                    session.save();
                    WorkflowManager manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
                    FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) manager.getWorkflow(descriptor);
                    workflow.depublish();
                    session.refresh(false);
                } catch(WorkflowException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                } catch(RemoteException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                } catch(RepositoryException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
        });

}/*

        add(new Label("caption", caption));

        TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel("hippostd:publishableSummary"));
        add(new Label("status", translator.getValueName("hippostd:stateSummary", new PropertyModel(this, "stateSummary"))));

        add(locked = new org.apache.wicket.markup.html.WebMarkupContainer("locked"));

        onModelChanged();

        addWorkflowAction("edit-dialog", new StringResourceModel("edit-label", this, null), new Visibility() {
            private static final long serialVersionUID = 1L;
            public boolean isVisible() {
                return !isLocked && !pendingRequest;
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;
            // Workaround for HREPTWO-1328
            @Override
            public void prepareSession(JcrNodeModel handleModel) throws RepositoryException {
                Node handleNode = handleModel.getNode();
                handleNode.getSession().refresh(false);
            }
            @Override
            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                ((UserSession) getSession()).getJcrSession().refresh(false);
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    editorMgr.openEditor(new JcrNodeModel(docNode));
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
            }
        });

        addWorkflowAction("publish-dialog", new StringResourceModel("publish-label", this, null), new Visibility() {
            private static final long serialVersionUID = 1L;
            public boolean isVisible() {
                // HREPTWO-2021
                // return !(stateSummary.equals("review") || stateSummary.equals("live")) && !pendingRequest;
                return false;
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;
            // Workaround for HREPTWO-1328
            @Override
            public void prepareSession(JcrNodeModel handleModel) throws RepositoryException {
                Node handleNode = handleModel.getNode();
                handleNode.getSession().refresh(false);
            }
            @Override
            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.publish();
            }
        });

        addWorkflowAction("dePublish-dialog", new StringResourceModel("depublish-label", this, null), new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                // HREPTWO-2021
                // return !(stateSummary.equals("review") || stateSummary.equals("new")) && !pendingRequest;
                return false;
            }
        }, new WorkflowAction() {
            private static final long serialVersionUID = 1L;
            // Workaround for HREPTWO-1328
            @Override
            public void prepareSession(JcrNodeModel handleModel) throws RepositoryException {
                Node handleNode = handleModel.getNode();
                handleNode.getSession().refresh(false);
            }
            @Override
            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.depublish();
            }
        });

        IModel deleteLabel = new StringResourceModel("delete-label", this, null);
        addWorkflowDialog("delete-dialog", deleteLabel, deleteLabel, new StringResourceModel("delete-message", this,
                null, new Object[] { caption }), new WorkflowAction() {
            private static final long serialVersionUID = 1L;

            // Workaround for HREPTWO-1328
            @Override
            public void prepareSession(JcrNodeModel handleModel) throws RepositoryException {
                Node handleNode = handleModel.getNode();
                handleNode.getSession().refresh(false);
            }

            @Override
            public void execute(Workflow wf) throws Exception {
                FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) wf;
                workflow.delete();
            }
        });

        IModel renameLabel = new StringResourceModel("rename-label", this, null);
        final StringResourceModel renameTitle = new StringResourceModel("rename-title", this, null);
        final StringResourceModel renameText = new StringResourceModel("rename-text", this, null);
        addWorkflowDialog("rename-dialog", renameLabel, new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return true;
            }}, new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {
                return new CompatibilityWorkflowPlugin.NameDialog(renameTitle, renameText, "") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String execute() {
                        try {
                            WorkflowDescriptorModel model = (WorkflowDescriptorModel) FullReviewedActionsWorkflowPlugin.this.getModel();
                            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                            FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) manager.getWorkflow((WorkflowDescriptor)(model.getObject()));
                            workflow.rename(NodeNameCodec.encode(name, true));
                            return null;
                        } catch(RepositoryException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(WorkflowException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(RemoteException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        }
                    }
                };
            }
        });

        IModel copyLabel = new StringResourceModel("copy-label", this, null);
        final StringResourceModel copyTitle = new StringResourceModel("copy-title", this, null);
        final StringResourceModel copyText = new StringResourceModel("copy-text", this, null);
        addWorkflowDialog("copy-dialog", copyLabel, new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return true;
            }}, new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {

                return new CompatibilityWorkflowPlugin.DestinationDialog(copyTitle, copyText) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String execute() {
                        try {
                            WorkflowDescriptorModel model = (WorkflowDescriptorModel) FullReviewedActionsWorkflowPlugin.this.getModel();
                            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                            FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) manager.getWorkflow((WorkflowDescriptor)(model.getObject()));
                            workflow.copy(new Document(destination.getNode().getUUID()), name);
                            return null;
                        } catch(RepositoryException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(WorkflowException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(RemoteException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        }
                    }
                };
            }
        });

        IModel moveLabel = new StringResourceModel("move-label", this, null);
        final StringResourceModel moveTitle = new StringResourceModel("move-title", this, null);
        final StringResourceModel moveText = new StringResourceModel("move-text", this, null);
        addWorkflowDialog("move-dialog", moveLabel, new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return true;
            }}, new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {

                return new CompatibilityWorkflowPlugin.DestinationDialog(moveTitle, moveText) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String execute() {
                        try {
                            WorkflowDescriptorModel model = (WorkflowDescriptorModel) FullReviewedActionsWorkflowPlugin.this.getModel();
                            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                            FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) manager.getWorkflow((WorkflowDescriptor)(model.getObject()));
                            workflow.move(new Document(destination.getNode().getUUID()), name);
                            return null;
                        } catch(RepositoryException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(WorkflowException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(RemoteException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        }
                    }
                };
            }
        });

        IModel schedulePublishLabel = new StringResourceModel("schedule-publish-label", this, null);
        final StringResourceModel schedulePublishTitle = new StringResourceModel("schedule-publish-title", this, null);
        final StringResourceModel schedulePublishText = new StringResourceModel("schedule-publish-text", this, null);
        addWorkflowDialog("schedule-publish-dialog", schedulePublishLabel, new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("live")) && !pendingRequest;

            }}, new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {

                return new DateDialog(schedulePublishText, new Date()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String execute() {
                        try {
                            WorkflowDescriptorModel model = (WorkflowDescriptorModel) FullReviewedActionsWorkflowPlugin.this.getModel();
                            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                            FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) manager.getWorkflow((WorkflowDescriptor)(model.getObject()));
                            if (date != null) {
                                workflow.publish(date);
                            } else {
                                workflow.publish();
                            }
                            return null;
                        } catch(RepositoryException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(WorkflowException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(RemoteException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        }
                    }

                    public IModel getTitle() {
                        return schedulePublishTitle;
                    }
                };
            }
        });

        IModel scheduleDePublishLabel = new StringResourceModel("schedule-depublish-label", this, null);
        final StringResourceModel scheduleDePublishTitle = new StringResourceModel("schedule-depublish-title", this, null);
        final StringResourceModel scheduleDePublishText = new StringResourceModel("schedule-depublish-text", this, null);
        addWorkflowDialog("schedule-depublish-dialog", scheduleDePublishLabel, new Visibility() {
            private static final long serialVersionUID = 1L;

            public boolean isVisible() {
                return !(stateSummary.equals("review") || stateSummary.equals("new")) && !pendingRequest;

            }}, new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {

                return new DateDialog(scheduleDePublishText, new Date()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String execute() {
                        try {
                            WorkflowDescriptorModel model = (WorkflowDescriptorModel) FullReviewedActionsWorkflowPlugin.this.getModel();
                            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                            FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) manager.getWorkflow((WorkflowDescriptor)(model.getObject()));
                            if (date != null) {
                                workflow.depublish(date);
                            } else {
                                workflow.depublish();
                            }
                            return null;
                        } catch(RepositoryException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(WorkflowException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        } catch(RemoteException ex) {
                            return ex.getClass().getName()+": "+ex.getMessage();
                        }
                    }

                    public IModel getTitle() {
                        return scheduleDePublishTitle;
                    }
                };
            }
        });
    }
*/}
