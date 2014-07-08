/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow.task;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.Request;

/**
 * Custom workflow task for deleting a request
 */
public class DeleteRequestTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private Request request;

    public Request getRequest() {
        return request;
    }

    public void setRequest(final Request request) {
        this.request = request;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        final Session session = getWorkflowContext().getInternalWorkflowSession();
        Node requestNode = request.getCheckedOutNode(session);
        JcrUtils.ensureIsCheckedOut(requestNode.getParent());
        requestNode.remove();
        session.save();
        return null;
    }
}
