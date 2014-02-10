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
package org.hippoecm.repository.reviewedactions;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;

/**
 * @deprecated since CMS 7.9, use/configure {@link org.onehippo.repository.documentworkflow.DocumentWorkflowImpl} instead.
 */
@Deprecated
public class FullRequestWorkflowImpl extends BasicRequestWorkflowImpl implements FullRequestWorkflow {

    /**
     * All implementations of a work-flow must provide a single, no-argument constructor.
     *
     * @throws java.rmi.RemoteException mandatory exception that must be thrown by all Remote objects
     */
    public FullRequestWorkflowImpl() throws RemoteException {
    }

    @Override
    public Map<String, Serializable> hints() throws WorkflowException {
        final Map<String, Serializable> hints = super.hints();
        try {
            Node node = getNode();
            String id = node.getIdentifier();
            Map<String, Map<String, Serializable>> requests = (Map<String, Map<String, Serializable>>) hints.get("requests");
            if (requests != null) {
                return requests.get(id);
            } else {
                return Collections.emptyMap();
            }
        } catch (RepositoryException e) {
            throw new WorkflowException("Unable to build request hints", e);
        }
    }

    // FullRequestWorkflow implementation

    @Override
    public void acceptRequest() throws WorkflowException, RemoteException, RepositoryException {
        documentWorkflow.acceptRequest(getNode().getIdentifier());
    }

    @Override
    public void rejectRequest(final String reason) throws WorkflowException, RemoteException, RepositoryException {
        documentWorkflow.rejectRequest(getNode().getIdentifier(), reason);
    }

}
