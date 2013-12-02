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
package org.onehippo.repository.documentworkflow;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom action for scheduling a publication or depublication of a document.
 */
public class ScheduleRequestAction extends AbstractDocumentAction {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ScheduleRequestAction.class);

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
    protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException,
            RepositoryException {

        DocumentHandle dm = getDataModel(scInstance);

        try {
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
                targetDate = eval(scInstance, getTargetDateExpr());
            }
            if (targetDate == null) {
                throw new WorkflowException("ScheduledRequestAction: no target date specified");
            }
            WorkflowContext wfCtx = getWorkflowContext(scInstance);
            wfCtx = wfCtx.getWorkflowContext(targetDate);
            FullReviewedActionsWorkflow wf = (FullReviewedActionsWorkflow) wfCtx.getWorkflow("default");

            if ("publish".equals(getType())) {
                wf.publish();
            }
            else {
                wf.depublish();
            }
        } catch (WorkflowException ex) {
            log.warn("no default workflow for published documents, falling back in behaviour", ex);
        } catch (RepositoryException ex) {
            log.warn("exception trying to archive document, falling back in behaviour", ex);
        } catch (RemoteException ex) {
            log.warn("exception trying to archive document, falling back in behaviour", ex);
        }
    }
}
