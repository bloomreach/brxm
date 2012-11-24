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

public class WorkflowDescriptorModel extends LoadableDetachableModel<WorkflowDescriptor> {

    private String uuid;
    private String relPath;
    private String category;

    public WorkflowDescriptorModel(WorkflowDescriptor descriptor, String category, Node subject) throws RepositoryException {
        super(descriptor);
        this.category = category;
        if (subject.isNodeType("mix:referenceable")) {
            this.uuid = subject.getUUID();
            this.relPath = null;
        } else {
            this.uuid = subject.getParent().getUUID();
            this.relPath = subject.getName();
            if(subject.getIndex() > 1) {
                this.relPath += "[" + subject.getIndex() + "]";
            }
        }
    }

    protected WorkflowDescriptor load() {
        try {
            Session session = UserSession.get().getJcrSession();
            WorkflowManager workflowManager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
            return workflowManager.getWorkflowDescriptor(category, getNode(session));
        } catch (RepositoryException ex) {
            return null;
        }
    }
    
    /** @deprecated by design FIXME */
    public Node getNode() throws RepositoryException {
        Session session = UserSession.get().getJcrSession();
        return getNode(session);
    }

    private Node getNode(Session session) throws RepositoryException {
        try {
            Node node = session.getNodeByUUID(uuid);
            if(relPath != null) {
                node = node.getNode(relPath);
            }
            return node;
        } catch(RepositoryException ex) {
            return null;
        }
    }

}
