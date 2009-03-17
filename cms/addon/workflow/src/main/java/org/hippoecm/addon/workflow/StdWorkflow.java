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
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
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

abstract class StdWorkflow extends ActionDescription {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    private String name;

    StdWorkflow(String id, String name) {
        super(id);
        this.name = name;

        add(new ActionDisplay("text") {
            @Override
            protected void initialize() {
                MenuLink link;
                add(link = new MenuLink("text") {
                    @Override
                    public void onClick() {
                        execute();
                    }
                });
                link.add(new Label("label", getTitle()));
            }
        });

        add(new ActionDisplay("icon") {
            @Override
            protected void initialize() {
                ResourceReference model = getIcon();
                add(new Image("icon", model));
            }
        });

        add(new ActionDisplay("panel") {
            @Override
            protected void initialize() {
            }
        });
    }
    
    protected final String getName() {
        return name;
    }

    protected StringResourceModel getTitle() {
        return new StringResourceModel(getName(), this, null, getName());
    }

    protected ResourceReference getIcon() {
        return new ResourceReference(getClass(), "workflow-16.png");
    }

    protected abstract void execute();
    
    public static abstract class Compatibility extends StdWorkflow {
        RenderPlugin enclosingPlugin;

        public Compatibility(String id, String name, RenderPlugin enclosingPlugin) {
            super(id, name);
            this.enclosingPlugin = enclosingPlugin;
        }

        protected abstract void execute(Workflow wf) throws Exception;

        protected void execute() {
            try {
                WorkflowDescriptor descriptor = (WorkflowDescriptor)enclosingPlugin.getModelObject();
                Session session = ((UserSession)getSession()).getJcrSession();
                session.refresh(true);
                session.save();
                WorkflowManager manager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
                Workflow workflow = manager.getWorkflow(descriptor);
                execute(workflow);
                session.refresh(false);
            } catch (WorkflowException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (ServiceException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RemoteException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (RepositoryException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            } catch (Exception ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }
}
