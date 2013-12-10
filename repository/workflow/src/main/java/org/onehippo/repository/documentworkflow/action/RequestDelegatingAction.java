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

import org.onehippo.repository.documentworkflow.task.RequestWorkflowTask;
import org.onehippo.repository.scxml.AbstractWorkflowTaskDelegatingAction;

/**
 * RequestDelegatingAction delegating the execution to RequestWorkflowTask.
 * <P>
 * Note: All the setters must be redefined to delegate to the RequestWorkflowTask.
 * </P>
 */
public class RequestDelegatingAction extends AbstractWorkflowTaskDelegatingAction<RequestWorkflowTask> {

    private static final long serialVersionUID = 1L;

    public String getType() {
        return getWorkflowTask().getType();
    }

    public void setType(String type) {
        getWorkflowTask().setType(type);
    }

    public String getContextVariantExpr() {
        return getWorkflowTask().getContextVariantExpr();
    }

    public void setContextVariantExpr(String contextVariantExpr) {
        getWorkflowTask().setContextVariantExpr(contextVariantExpr);
    }

    public String getTargetDateExpr() {
        return getWorkflowTask().getTargetDateExpr();
    }

    public void setTargetDateExpr(String targetDateExpr) {
        getWorkflowTask().setTargetDateExpr(targetDateExpr);
    }

    @Override
    protected RequestWorkflowTask createWorkflowTask() {
        return new RequestWorkflowTask();
    }

}
