/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.util.JcrUtils;

/**
 * Deprecated JCR-based implementation.  Kept for reference for new SCXML based implementation.
 */
@Deprecated
public class JCRBasicRequestWorkflowImpl extends WorkflowImpl implements BasicRequestWorkflow {

    protected PublicationRequest request;

    public JCRBasicRequestWorkflowImpl() throws RemoteException {
    }

    @Override
    public void setNode(Node node) throws RepositoryException {
        super.setNode(node);
        this.request = node != null ? new PublicationRequest(node) : null;
    }

    @Override
    public Map<String,Serializable> hints() throws WorkflowException {
        Map<String,Serializable> info = super.hints();
        try {
            if(request.getOwner() != null) {
                if(request.getOwner().equals(getWorkflowContext().getUserIdentity())) {
                    info.put("cancelRequest", true);
                } else {
                    info.put("cancelRequest", false);
                }
            }
        }
        catch (RepositoryException ex) {
            // TODO DEJDO: ignore?
        }
        return info;
    }

    public void cancelRequest() throws WorkflowException, RepositoryException {
        if (request != null) {
            Node requestNode = request.getCheckedOutNode();
            JcrUtils.ensureIsCheckedOut(requestNode.getParent());
            requestNode.remove();
            request = null;
        }
    }
}
