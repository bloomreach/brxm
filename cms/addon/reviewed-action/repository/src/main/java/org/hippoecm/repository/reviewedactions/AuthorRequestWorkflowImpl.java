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

import org.hippoecm.repository.Utilities;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowMappingException;
import org.hippoecm.repository.servicing.WorkflowImpl;

public class AuthorRequestWorkflowImpl extends WorkflowImpl implements AuthorRequestWorkflow {

    public ReviewedActionsWorkflowImpl workflow;
    public PublicationRequest request;
    PublishableDocument document;

    public AuthorRequestWorkflowImpl() throws RemoteException {
    }

    public void cancelRequest() throws WorkflowException, WorkflowMappingException, RepositoryException {
        document = null;
        request = null;
    }

}
