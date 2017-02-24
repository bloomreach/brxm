/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.task.WorkflowRequestTask;

/**
 * WorkflowRequestAction is a custom DocumentWorkflow SCXML state machine action for creating a new
 * document workflow request node of a specific type (publish, depublish, scheduledpublish, scheduleddepublish, delete)
 * for the current document, optionally (depending on the type) on a specific target date.
 * <p>
 * The execution of this task is delegated to its corresponding {@link WorkflowRequestTask}.
 * </p>
 */
public class WorkflowRequestAction extends AbstractDocumentTaskAction<WorkflowRequestTask> {

    private static final long serialVersionUID = 1L;

    static final String PUBLICATION_DATE_EXPR = "publicationDateExpr";
    static final String DEPUBLICATION_DATE_EXPR = "depublicationDateExpr";

    public String getType() {
        return getParameter("type");
    }

    @SuppressWarnings("unused")
    public void setType(String type) {
        setParameter("type", type);
    }

    public String getContextVariantExpr() {
        return getParameter("contextVariantExpr");
    }

    @SuppressWarnings("unused")
    public void setContextVariantExpr(String contextVariantExpr) {
        setParameter("contextVariantExpr", contextVariantExpr);
    }

    public String getPublicationDateExpr() {
        return getParameter(PUBLICATION_DATE_EXPR);
    }

    public void setPublicationDateExpr(String publishDateExpr) {
        setParameter(PUBLICATION_DATE_EXPR, publishDateExpr);
    }

    public String getDepublicationDateExpr() {
        return getParameter(DEPUBLICATION_DATE_EXPR);
    }

    public void setDepublicationDateExpr(String unpublicationDateExpr) {
        setParameter(DEPUBLICATION_DATE_EXPR, unpublicationDateExpr);
    }

    @Override
    protected WorkflowRequestTask createWorkflowTask() {
        return new WorkflowRequestTask();
    }

    @Override
    protected void initTask(WorkflowRequestTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setType(getType());
        task.setContextVariant((eval(getContextVariantExpr())));
        task.setPublicationDate(eval(getPublicationDateExpr()));
        task.setDepublicationDate(eval(getDepublicationDateExpr()));
    }
}
