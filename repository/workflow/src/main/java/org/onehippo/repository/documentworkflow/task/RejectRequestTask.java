/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.WorkflowRequest;

/**
 * Custom workflow task for rejecting a request
 */
public class RejectRequestTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private WorkflowRequest request;
    private String reason;

    public WorkflowRequest getRequest() {
        return request;
    }

    public void setRequest(final WorkflowRequest request) {
        this.request = request;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        getRequest().setRejected(getReason());
        return null;
    }
}
