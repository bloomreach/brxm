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
import org.onehippo.repository.documentworkflow.PublishableDocument;
import org.onehippo.repository.documentworkflow.task.RequestTask;

/**
 * RequestAction delegating the execution to RequestTask.
 */
public class RequestAction extends AbstractDocumentTaskAction<RequestTask> {

    private static final long serialVersionUID = 1L;

    public String getType() {
        return getParameter("type");
    }

    public void setType(String type) {
        setParameter("type", type);
    }

    public String getContextVariantExpr() {
        return getParameter("contextVariantExpr");
    }

    public void setContextVariantExpr(String contextVariantExpr) {
        setParameter("contextVariantExpr", contextVariantExpr);
    }

    public String getTargetDateExpr() {
        return getParameter("targetDateExpr");
    }

    public void setTargetDateExpr(String targetDateExpr) {
        setParameter("targetDateExpr", targetDateExpr);
    }

    @Override
    protected RequestTask createWorkflowTask() {
        return new RequestTask();
    }

    @Override
    protected void initTask(RequestTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setType(getType());
        task.setContextVariant((PublishableDocument) eval(getContextVariantExpr()));
        task.setTargetDate((Date) eval(getTargetDateExpr()));
    }
}
