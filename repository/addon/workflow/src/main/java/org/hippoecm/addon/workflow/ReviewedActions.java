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
package org.hippoecm.addon.workflow;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService; 
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;

public class ReviewedActions extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    // public ReviewedActions(String id, final WorkflowDescriptorModel model) super(id, model);
    public ReviewedActions(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow.Compatibility("edit", "edit", this) {
            protected void execute(Workflow workflow) throws Exception {
                FullReviewedActionsWorkflow wf = (FullReviewedActionsWorkflow)workflow;
                Document docRef = wf.obtainEditableInstance();
                Session session = ((UserSession)getSession()).getJcrSession();
                session.refresh(false);
                Node docNode = session.getNodeByUUID(docRef.getIdentity());
                IEditorManager editorMgr = getPluginContext().getService(getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    editorMgr.openEditor(new JcrNodeModel(docNode));
                } else {
                    System.err.println("No editor found to edit " + docNode.getPath());
                }
            }
        });
        
        add(new StdWorkflow("delete", "delete") {
            protected void execute() {
                try {
                    WorkflowDescriptor descriptor = (WorkflowDescriptor) ReviewedActions.this.getModelObject();
                    Session session = ((UserSession) getSession()).getJcrSession();
                    session.refresh(true);
                    session.save();
                    WorkflowManager manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
                    DefaultWorkflow workflow = (DefaultWorkflow) manager.getWorkflow(descriptor);
                    workflow.delete();
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

        add(new StdWorkflow("copy", "copy") {
            protected void execute() {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(new AbstractDialog() {

                    public IModel getTitle() {
                        return new Model("Sure");
                    }
                });
            }
        });

        add(new StdWorkflow("move", "move") {
            protected void execute() {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(new AbstractDialog() {

                    public IModel getTitle() {
                        return new Model("Sure");
                    }
                });
            }
        });

        add(new StdWorkflow("rename", "rename") {
            protected void execute() {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(new AbstractDialog() {

                    public IModel getTitle() {
                        return new Model("Sure");
                    }
                });
            }
        });

        add(new StdWorkflow("publish", "publish") {
            protected void execute() {
                try {
                    WorkflowDescriptor descriptor = (WorkflowDescriptor) ReviewedActions.this.getModelObject();
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
            protected void execute() {
                try {
                    WorkflowDescriptor descriptor = (WorkflowDescriptor) ReviewedActions.this.getModelObject();
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
    }
}
