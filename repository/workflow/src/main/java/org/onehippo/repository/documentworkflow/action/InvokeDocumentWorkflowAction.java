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
import org.onehippo.repository.documentworkflow.task.InvokeDocumentWorkflowTask;

/**
 * InvokeDocumentWorkflowAction delegating the execution to InvokeDocumentWorkflowTask.
 */
public class InvokeDocumentWorkflowAction extends AbstractDocumentTaskAction<InvokeDocumentWorkflowTask> {

    private static final long serialVersionUID = 1L;

    public String getAction() {
        return getParameter("action");
    }

    public void setAction(String action) {
        setParameter("action", action);
    }

    public String getWhenExpr() {
        return getParameter("whenExpr");
    }

    public void setWhenExpr(String whenExpr) {
        setParameter("whenExpr", whenExpr);
    }

    public String getSubjectExpr() {
        return getParameter("subjectExpr");
    }

    public void setSubjectExpr(String subject) {
        setParameter("subjectExpr", subject);
    }

    @Override
    protected InvokeDocumentWorkflowTask createWorkflowTask() {
        return new InvokeDocumentWorkflowTask();
    }

    @Override
    protected void initTask(InvokeDocumentWorkflowTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);

        task.setAction(getAction());
        if (getWhenExpr() != null) {
            task.setWhen((Date)eval(getWhenExpr()));
        }
        task.setSubject((PublishableDocument) eval(getSubjectExpr()));
    }
}
