/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.standardworkflow;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.reviewedactions.AbstractReviewedActionsWorkflow;
import org.onehippo.repository.util.JcrConstants;

public class VersionWorkflowImpl extends AbstractReviewedActionsWorkflow implements VersionWorkflow {

    public VersionWorkflowImpl() throws RemoteException {
    }

    protected Node getSubjectHandleNode() throws RepositoryException {
        Node node = getNode();
        // override needed to handle dereferencing document handle for a jcr:frozenNode subject
        if (node.isNodeType(JcrConstants.JCR_FROZEN_NODE)) {
            Node subject = node.getSession().getNodeByIdentifier(node.getProperty(JcrConstants.JCR_FROZEN_UUID).getString());
            Node handleNode = subject.getParent();
            if (!handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                throw new RepositoryException("Invalid workflow subject " + node.getPath() + ", is not a frozen node referencing a node with a handle as parent");
            }
            return handleNode;
        }
        else {
            return super.getSubjectHandleNode();
        }
    }

    // VersionWorkflow implementation

    @Override
    public Document version() throws WorkflowException, RepositoryException, RemoteException {
        return handleDocumentWorkflow.version();
    }

    @Override
    public Document revert(final Calendar historic) throws WorkflowException, RepositoryException, RemoteException {
        return handleDocumentWorkflow.revertFromVersion(historic);
    }

    @Override
    public Document restoreTo(final Document target) throws WorkflowException, RepositoryException, RemoteException {
        return handleDocumentWorkflow.restoreFromVersion((Version)getNode().getParent());
    }

    @Override
    public Document restore(final Calendar historic) throws WorkflowException, RepositoryException, RemoteException {
        return handleDocumentWorkflow.restoreFromVersion(historic);
    }

    @Override
    public Document restore(final Calendar historic, final Map<String,String[]> replacements) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    @Override
    public SortedMap<Calendar, Set<String>> list() throws WorkflowException, RemoteException, RepositoryException {
        return handleDocumentWorkflow.listVersions();
    }

    @Override
    public Document retrieve(final Calendar historic) throws WorkflowException, RepositoryException, RemoteException {
        return handleDocumentWorkflow.retrieveVersion(historic);
    }

}
