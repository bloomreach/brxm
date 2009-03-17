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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.model.LoadableDetachableModel;

import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class WorkflowDescriptorModel extends LoadableDetachableModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    String path;
    String category;

    public WorkflowDescriptorModel(WorkflowDescriptor descriptor, String category, Node subject) throws RepositoryException {
        super(descriptor);
        this.category = category;
        this.path = subject.getPath();
    }

    protected Object load() {
        try {
            Session session = ((UserSession)org.apache.wicket.Session.get()).getJcrSession();
            WorkflowManager workflowManager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
            return workflowManager.getWorkflowDescriptor(category, session.getRootNode().getNode(path.substring(1)));
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
            return null;
        }
    }
    
    /** @deprecated */
    public Node getNode() throws RepositoryException {
        Session session = ((UserSession)org.apache.wicket.Session.get()).getJcrSession();
        return session.getRootNode().getNode(path.substring(1));
    }
}
