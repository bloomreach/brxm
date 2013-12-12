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

import org.onehippo.repository.documentworkflow.PublishableDocument;
import org.onehippo.repository.documentworkflow.task.RequestTask;
import org.onehippo.repository.scxml.AbstractTaskAction;

/**
 * RequestAction delegating the execution to RequestTask.
 */
public class RequestAction extends AbstractTaskAction<RequestTask> {

    private static final long serialVersionUID = 1L;

    public String getType() {
        return getPropertiesMap().get("type");
    }

    public void setType(String type) {
        getPropertiesMap().put("type", type);
    }

    public String getContextVariantExpr() {
        return (String) getRuntimePropertiesMap().get("contextVariant");
    }

    public void setContextVariantExpr(String contextVariantExpr) {
        getRuntimePropertiesMap().put("contextVariant", contextVariantExpr);
    }

    public String getTargetDateExpr() {
        return (String) getRuntimePropertiesMap().get("targetDate");
    }

    public void setTargetDateExpr(String targetDateExpr) {
        getRuntimePropertiesMap().put("targetDate", targetDateExpr);
    }

    @Override
    protected RequestTask createWorkflowTask() {
        return new RequestTask();
    }

    @Override
    protected void initTaskBeforeEvaluation(RequestTask task, Map<String, String> propertiesMap) {
        super.initTaskBeforeEvaluation(task, propertiesMap);
        task.setType(propertiesMap.get("type"));
    }

    @Override
    protected void initTaskAfterEvaluation(RequestTask task, Map<String, Object> runtimePropertiesMap) {
        super.initTaskAfterEvaluation(task, runtimePropertiesMap);
        task.setContextVariant((PublishableDocument) runtimePropertiesMap.get("contextVariant"));
        task.setTargetDate((Date) runtimePropertiesMap.get("targetDate"));
    }
}
