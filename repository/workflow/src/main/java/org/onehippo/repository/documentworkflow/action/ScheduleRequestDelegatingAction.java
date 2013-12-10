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

import org.onehippo.repository.documentworkflow.task.ScheduleRequestWorkflowTask;
import org.onehippo.repository.scxml.AbstractWorkflowTaskDelegatingAction;

/**
 * ScheduleRequestDelegatingAction delegating the execution to ScheduleRequestWorkflowTask.
 * <P>
 * Note: All the setters must be redefined to delegate to the ScheduleRequestWorkflowTask.
 * </P>
 */
public class ScheduleRequestDelegatingAction extends AbstractWorkflowTaskDelegatingAction<ScheduleRequestWorkflowTask> {

    private static final long serialVersionUID = 1L;

    public String getType() {
        return getWorkflowTask().getType();
    }

    public void setType(final String type) {
        getWorkflowTask().setType(type);
    }

    public String getTargetDateExpr() {
        return getWorkflowTask().getTargetDateExpr();
    }

    public void setTargetDateExpr(final String targetDateExpr) {
        getWorkflowTask().setTargetDateExpr(targetDateExpr);
    }

    @Override
    protected ScheduleRequestWorkflowTask createWorkflowTask() {
        return new ScheduleRequestWorkflowTask();
    }

}
