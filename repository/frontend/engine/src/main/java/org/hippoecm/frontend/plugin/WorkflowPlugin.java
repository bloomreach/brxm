/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugin;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.WorkflowMappingException;

public abstract class WorkflowPlugin extends Plugin {

    //TODO:  wrap these in detachable models instead of making them transient
    private transient WorkflowManager workflowManager;
    private transient WorkflowDescriptor workflowDescriptor;
    
    protected interface Callback extends IClusterable {
        void execute(Workflow workflow) throws WorkflowMappingException, RemoteException, WorkflowException, RepositoryException;
    }

    public WorkflowPlugin(String id, JcrNodeModel model, WorkflowManager workflowManager, WorkflowDescriptor workflowDescriptor) {
        super(id, model);
        this.workflowManager = workflowManager;
        this.workflowDescriptor = workflowDescriptor;
    }

    public WorkflowManager getWorkflowManager() {
        return workflowManager;
    }

    public WorkflowDescriptor getWorkflowDescriptor() {
        return workflowDescriptor;
    }
    
    protected Workflow getWorkflow() {
        Workflow workflow = null;
        try {
            workflow = getWorkflowManager().getWorkflow(workflowDescriptor);
        } catch (WorkflowMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return workflow;
    }
    
    protected Component workflowMethodCaller(String id, final JcrNodeModel model, String label, final Callback callback) {
        Component result = null;
        if (model == null || model.getNode() == null) {
            //TODO: ask permission to execute this method, if not allowed show this label 
            result = new Label(id, label);
        } else {
            result = new AjaxLink(id, model) {
                private static final long serialVersionUID = 1L;
                public void onClick(AjaxRequestTarget target) {
                    try {
                        callback.execute(getWorkflow());
                    } catch (WorkflowMappingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (WorkflowException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (RepositoryException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            };
        }
        return result;   
    }
}
