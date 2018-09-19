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

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.task.ListVersionsVariantTask;

/**
 * ListVersionsVariantAction is a custom DocumentWorkflow SCXML state machine action for reporting the available
 * versions of a specific document variant (e.g. the unpublished variant) as SCXML state machine result through the
 * {@link org.onehippo.repository.scxml.SCXMLWorkflowContext#getResult()} object.
 * <p>
 * The execution of this task is delegated to its corresponding {@link ListVersionsVariantTask}.
 * </p>
 */
public class ListVersionsVariantAction extends AbstractDocumentTaskAction<ListVersionsVariantTask> {

    private static final long serialVersionUID = 1L;

    public String getVariant() {
        return getParameter("variantExpr");
    }

    @SuppressWarnings("unused")
    public void setVariant(String variant) {
        setParameter("variantExpr", variant);
    }

    @Override
    protected ListVersionsVariantTask createWorkflowTask() {
        return new ListVersionsVariantTask();
    }

    @Override
    protected void initTask(ListVersionsVariantTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setVariant((DocumentVariant) eval(getVariant()));
    }
}
