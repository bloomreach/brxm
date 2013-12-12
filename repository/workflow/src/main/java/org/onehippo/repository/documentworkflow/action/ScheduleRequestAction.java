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
import java.util.Map;

import org.onehippo.repository.documentworkflow.task.ScheduleRequestTask;
import org.onehippo.repository.scxml.AbstractTaskAction;

/**
 * ScheduleRequestAction delegating the execution to ScheduleRequestTask.
 */
public class ScheduleRequestAction extends AbstractTaskAction<ScheduleRequestTask> {

    private static final long serialVersionUID = 1L;

    public String getType() {
        return getPropertiesMap().get("type");
    }

    public void setType(final String type) {
        getPropertiesMap().put("type", type);
    }

    public String getTargetDateExpr() {
        return (String) getRuntimePropertiesMap().get("targetDate");
    }

    public void setTargetDateExpr(final String targetDateExpr) {
        getRuntimePropertiesMap().put("targetDate", targetDateExpr);
    }

    @Override
    protected ScheduleRequestTask createWorkflowTask() {
        return new ScheduleRequestTask();
    }

    @Override
    protected void initTaskBeforeEvaluation(ScheduleRequestTask task, Map<String, String> propertiesMap) {
        super.initTaskBeforeEvaluation(task, propertiesMap);
    }

    @Override
    protected void initTaskAfterEvaluation(ScheduleRequestTask task, Map<String, Object> runtimePropertiesMap) {
        super.initTaskAfterEvaluation(task, runtimePropertiesMap);
        task.setTargetDate((Date) runtimePropertiesMap.get("targetDate"));
    }
}
