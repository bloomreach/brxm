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

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.MappingException;

/**
  * @deprecated since CMS 7.9, use/configure {@link org.onehippo.repository.documentworkflow.HandleDocumentWorkflow} instead.
 */
@Deprecated
public interface FullRequestWorkflow extends BasicRequestWorkflow {

    /**
     * Approve and execute or schedule request
     */
    public void acceptRequest()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Rejects request with given reason
     */
    public void rejectRequest(String reason)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
