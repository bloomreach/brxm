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
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom workflow task for scheduling a publication or depublication of a document.
 */
public class ScheduleRequestWorkflowTask extends AbstractDocumentWorkflowTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ScheduleRequestWorkflowTask.class);

    private String type;
    private String targetDateExpr;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getTargetDateExpr() {
        return targetDateExpr;
    }

    public void setTargetDateExpr(final String targetDateExpr) {
        this.targetDateExpr = targetDateExpr;
    }

    @Override
    public void doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dm = getDataModel();

        if (getType() == null || !("publish".equals(getType()) || "depublish".equals(getType()))) {
            throw new WorkflowException("Unknown or undefined ScheduledRequestAction: "+getType());
        }
        Boolean allowed = null;
        try {
            allowed = (Boolean)dm.getHints().get(getType());
        }
        catch (Exception e) {
            //
        }
        if (allowed == null || !allowed.booleanValue()) {
            throw new WorkflowException("ScheduledRequestAction: "+getType()+" not allowed");
        }
        Date targetDate = null;
        if (getTargetDateExpr() != null) {
            targetDate = eval(getTargetDateExpr());
        }
        if (targetDate == null) {
            throw new WorkflowException("ScheduledRequestAction: no target date specified");
        }
        WorkflowContext wfCtx = getWorkflowContext();
        wfCtx = wfCtx.getWorkflowContext(targetDate);
        FullReviewedActionsWorkflow wf = (FullReviewedActionsWorkflow) wfCtx.getWorkflow("default");

        if ("publish".equals(getType())) {
            wf.publish();
        }
        else {
            wf.depublish();
        }
    }
}
