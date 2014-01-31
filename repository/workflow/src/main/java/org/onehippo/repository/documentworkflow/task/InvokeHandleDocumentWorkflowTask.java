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
import java.util.Date;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.HandleDocumentWorkflow;

/**
 * Custom workflow task for invoking a HandleDocumentWorkflow action
 */
public class InvokeHandleDocumentWorkflowTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private Date when;
    private String action;
    private DocumentVariant subject;

    public Date getWhen() {
        return when;
    }

    public void setWhen(final Date when) {
        this.when = when;
    }

    public String getAction() {
        return action;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        DocumentHandle dh = getDocumentHandle();
        WorkflowContext wfc = dh.getWorkflowContext();
        Document handle = new Document(dh.getHandle());
        if ("delete".equals(action)) {
            wfc.getInternalWorkflowSession().save();
            ((HandleDocumentWorkflow)wfc.getWorkflow("default", handle)).delete();
        }
        else if ("publish".equals(action)) {
            wfc.getInternalWorkflowSession().save();
            ((HandleDocumentWorkflow)wfc.getWorkflow("default", handle)).publish();
        }
        else if ("depublish".equals(action)) {
            wfc.getInternalWorkflowSession().save();
            ((HandleDocumentWorkflow)wfc.getWorkflow("default", handle)).depublish();
        }
        else if ("scheduledpublish".equals(action)) {
            wfc.getInternalWorkflowSession().save();
            ((HandleDocumentWorkflow)wfc.getWorkflow("default", handle)).publish(when);
        }
        else if ("scheduleddepublish".equals(action)) {
            wfc.getInternalWorkflowSession().save();
            ((HandleDocumentWorkflow)wfc.getWorkflow("default", handle)).depublish(when);
        }
        else {
            throw new WorkflowException("Unsupported workflow action: "+(action));
        }
        return null;
    }
}
