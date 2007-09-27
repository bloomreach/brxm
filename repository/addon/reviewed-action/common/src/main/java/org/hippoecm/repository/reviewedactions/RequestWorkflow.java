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
package org.hippoecm.repository.reviewedactions;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowMappingException;

public interface RequestWorkflow extends Workflow {
    /**
     * Cancels and/or disposes (!) the request.
     */
    public void cancelRequest()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Approve and execute or schedule request
     */
    public void acceptRequest()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Rejects request with given reason
     */
    public void rejectRequest(String reason)
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;
}
