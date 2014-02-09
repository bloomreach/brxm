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

import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom workflow task for scheduling a publication or depublication of a document.
 */
public class ScheduleWorkflowTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ScheduleWorkflowTask.class);

    private String type;
    private Date targetDate;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Date getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dh = getDocumentHandle();

        if (getType() == null || !("publish".equals(getType()) || "depublish".equals(getType()))) {
            throw new WorkflowException("Unknown or undefined ScheduledWorkflowAction: "+getType());
        }
        if (targetDate == null) {
            throw new WorkflowException("ScheduledWorkflowAction: no target date specified");
        }
        WorkflowContext wfc = dh.getWorkflowContext();
        // create 'future' documentworkflow proxy
        WorkflowContext futureContext = wfc.getWorkflowContext(targetDate);
        DocumentWorkflow workflow = (DocumentWorkflow)futureContext.getWorkflow("default");

        if ("publish".equals(getType())) {
            workflow.publish();
        }
        else {
            workflow.depublish();
        }

        return null;
    }
}
