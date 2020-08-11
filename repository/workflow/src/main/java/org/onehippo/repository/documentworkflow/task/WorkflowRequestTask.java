/**
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
import java.util.Date;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.WorkflowRequest;

/**
 * Custom workflow task for requesting a publish, depublish, schedpublish, scheddepublish or delete operation of
 * a document.
 */
public class WorkflowRequestTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private String type;
    private DocumentVariant contextVariant;
    private Date targetDate;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @SuppressWarnings("unused")
    public DocumentVariant getContextVariant() {
        return contextVariant;
    }

    public void setContextVariant(DocumentVariant contextVariant) {
        this.contextVariant = contextVariant;
    }

    @SuppressWarnings("unused")
    public Date getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dm = getDocumentHandle();

        if (!dm.isRequestPending()) {

            if (targetDate == null) {
                new WorkflowRequest(getType(), contextVariant.getNode(getWorkflowContext().getInternalWorkflowSession()),
                        contextVariant, getWorkflowContext().getUserIdentity());
            } else {
                new WorkflowRequest(getType(), contextVariant.getNode(getWorkflowContext().getInternalWorkflowSession()),
                        contextVariant, getWorkflowContext().getUserIdentity(), targetDate);
            }

            getWorkflowContext().getInternalWorkflowSession().save();
        } else {
            throw new WorkflowException("publication request already pending");
        }

        return null;
    }

}
