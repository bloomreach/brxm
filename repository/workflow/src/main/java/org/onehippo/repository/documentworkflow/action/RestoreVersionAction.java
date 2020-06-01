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

import java.util.Calendar;

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.task.RestoreVersionTask;

/**
 * RestoreVersionAction is a custom DocumentWorkflow SCXML state machine action for restoring a specific document
 * version to a document variant (e.g. unpublished), using the standard JCR version restore functionality.
 * <p>
 * The resulting {@link DocumentVariant} document object is returned as SCXML state machine execution
 * {@link org.onehippo.repository.scxml.SCXMLWorkflowContext#getResult() result}.
 * </p>
 * <p>
 * Note the difference of this action compared to {@link VersionRestoreToAction} which uses a custom/manual copying
 * of the version content onto its target node.
 * </p>
 * <p>
 * The execution of this task is delegated to its corresponding {@link RestoreVersionTask}.
 * </p>
 */
public class RestoreVersionAction extends AbstractDocumentTaskAction<RestoreVersionTask> {

    private static final long serialVersionUID = 1L;

    public String getVariant() {
        return getParameter("variantExpr");
    }

    @SuppressWarnings("unused")
    public void setVariant(String variant) {
        setParameter("variantExpr", variant);
    }

    public String getHistoric() {
        return getParameter("historicExpr");
    }

    @SuppressWarnings("unused")
    public void setHistoric(String variant) {
        setParameter("historicExpr", variant);
    }

    @Override
    protected RestoreVersionTask createWorkflowTask() {
        return new RestoreVersionTask();
    }

    @Override
    protected void initTask(RestoreVersionTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setVariant((DocumentVariant) eval(getVariant()));
        task.setHistoric((Calendar) eval(getHistoric()));
    }

}
