/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.api.Document;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.task.VersionRestoreToTask;

/**
 * VersionRestoreToAction is a custom DocumentWorkflow SCXML state machine action for restoring a specific document
 * version to a specific target document using a custom/manual copying of the version content onto the target node.
 * <p>
 * The resulting {@link DocumentVariant} document object is returned as SCXML state machine execution
 * {@link org.onehippo.repository.scxml.SCXMLWorkflowContext#getResult() result}.
 * </p>
 * <p>
 * Note the difference of this action compared to {@link RestoreVersionAction} which uses the standard JCR version
 * restore functionality.
 * </p>
 * <p>
 * The execution of this task is delegated to its corresponding {@link VersionRestoreToTask}.
 * </p>
 */
public class VersionRestoreToAction extends AbstractDocumentTaskAction<VersionRestoreToTask> {

    private static final long serialVersionUID = 1L;

    public String getVariant() {
        return getParameter("variantExpr");
    }

    @SuppressWarnings("unused")
    public void setVariant(String variant) {
        setParameter("variantExpr", variant);
    }

    public String getTarget() {
        return getParameter("targetExpr");
    }

    @SuppressWarnings("unused")
    public void setTarget(String targetExpr) {
        setParameter("targetExpr", targetExpr);
    }

    public String getHistoric() {
        return getParameter("historicExpr");
    }

    @SuppressWarnings("unused")
    public void setHistoric(String variant) {
        setParameter("historicExpr", variant);
    }

    @Override
    protected VersionRestoreToTask createWorkflowTask() {
        return new VersionRestoreToTask();
    }

    @Override
    protected void initTask(VersionRestoreToTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setTarget((Document) eval(getTarget()));
        task.setVariant((DocumentVariant) eval(getVariant()));
        task.setHistoric((Calendar) eval(getHistoric()));
    }
}
