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
import org.hippoecm.repository.api.Document;
import org.onehippo.repository.documentworkflow.task.MoveDocumentTask;

/**
 * MoveDocumentAction is a custom DocumentWorkflow SCXML state machine action for moving the current document (handle)
 * to a new destination folder under a specific (possibly different) name.
 * <p>
 * The execution of this task is delegated to its corresponding {@link MoveDocumentTask}.
 * </p>
 */
public class MoveDocumentAction extends AbstractDocumentTaskAction<MoveDocumentTask> {

    private static final long serialVersionUID = 1L;

    public String getDestinationExpr() {
        return getParameter("destinationExpr");
    }

    @SuppressWarnings("unused")
    public void setDestinationExpr(String destinationExpr) {
        setParameter("destinationExpr", destinationExpr);
    }

    public String getNewNameExpr() {
        return getParameter("newNameExpr");
    }

    @SuppressWarnings("unused")
    public void setNewNameExpr(String newNameExpr) {
        setParameter("newNameExpr", newNameExpr);
    }

    @Override
    protected MoveDocumentTask createWorkflowTask() {
        return new MoveDocumentTask();
    }

    @Override
    protected void initTask(MoveDocumentTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setDestination((Document) eval(getDestinationExpr()));
        task.setNewName((String) eval(getNewNameExpr()));
    }
}
