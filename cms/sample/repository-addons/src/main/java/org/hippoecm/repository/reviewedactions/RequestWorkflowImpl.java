/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

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

public class RequestWorkflowImpl extends WorkflowImpl implements RequestWorkflow {
    PublicationRequest request;
    ReviewedActionsWorkflow document;

    public RequestWorkflowImpl() throws RemoteException {
    }

    public void cancelRequest() throws WorkflowException, WorkflowMappingException, RepositoryException {
        document = null;
        request = null;
    }

    public void acceptRequest() throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException {
        if(request.DELETE.equals(request.type)) {
            document.delete();
        } else if(request.PUBLISH.equals(request.type)) {
            document.publish();
        } else if(request.DEPUBLISH.equals(request.type)) {
            document.depublish();
        } else if(request.REJECTED.equals(request.type)) {
            throw new WorkflowException("request has already been rejected");
        } else
            throw new WorkflowMappingException("unknown publication request");
    }

    public void rejectRequest(String reason) throws WorkflowException, WorkflowMappingException, RepositoryException {
        request.reason = reason;
        request.type = request.REJECTED;
    }
}
