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
package org.hippoecm.repository.decorating.checked;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class WorkflowManagerDecorator implements WorkflowManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    Session session;
    WorkflowManager workflowManager;

    protected WorkflowManagerDecorator(Session session, WorkflowManager workflowManager) {
        this.session = session;
        this.workflowManager = workflowManager;
    }

    protected void check() throws RepositoryException {
        if(!SessionDecorator.unwrap(session).isLive()) {
            this.workflowManager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        }
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
        check();
        return workflowManager.getWorkflowDescriptor(category, item);
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Document document) throws RepositoryException {
        check();
        return workflowManager.getWorkflowDescriptor(category, document);
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
        check();
        return workflowManager.getWorkflow(descriptor);
    }

    public Workflow getWorkflow(String category, Node item) throws RepositoryException {
        check();
        return workflowManager.getWorkflow(category, item);
    }

    public Workflow getWorkflow(String category, Document document) throws RepositoryException {
        check();
        return workflowManager.getWorkflow(category, document);
    }
}
