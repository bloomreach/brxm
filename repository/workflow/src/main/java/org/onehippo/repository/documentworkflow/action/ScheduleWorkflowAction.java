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

package org.onehippo.repository.documentworkflow.action;

import java.util.Date;

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.task.ScheduleWorkflowTask;

/**
 * ScheduleWorkflowAction is a custom DocumentWorkflow SCXML state machine action for scheduling a "publish" or
 * "depublish" {@link #setType(String) type} workflow operation of the current document on a specific
 * {@link #setTargetDateExpr(String) date}.
 * <p>
 * The execution of this task is delegated to its corresponding {@link ScheduleWorkflowTask}.
 * </p>
 */
public class ScheduleWorkflowAction extends AbstractDocumentTaskAction<ScheduleWorkflowTask> {

    private static final long serialVersionUID = 1L;

    public String getType() {
        return getParameter("type");
    }

    @SuppressWarnings("unused")
    public void setType(String type) {
        setParameter("type", type);
    }

    public String getTargetDateExpr() {
        return getParameter("targetDateExpr");
    }

    @SuppressWarnings("unused")
    public void setTargetDateExpr(String targetDateExpr) {
        setParameter("targetDateExpr", targetDateExpr);
    }

    @Override
    protected ScheduleWorkflowTask createWorkflowTask() {
        return new ScheduleWorkflowTask();
    }

    @Override
    protected void initTask(ScheduleWorkflowTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setType(getType());
        task.setTargetDate((Date) eval(getTargetDateExpr()));
    }

}
