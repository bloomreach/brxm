/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.repository.documentworkflow.task.SetTransferableTask;

/**
 * SetTransferableAction is a custom DocumentWorkflow SCXML state machine action for setting or removing the current
 * draft document variant transferable.
 * <p>
 * The execution of this task is delegated to its corresponding {@link SetTransferableTask}.
 * </p>
 */
public final class SetTransferableAction extends AbstractDocumentTaskAction<SetTransferableTask> {

    private static final long serialVersionUID = 1L;

    /**
     * Reads the transferable property from the scxml transferable tag.
     *
     * @param transferable as read from the matching scxml tag
     */
    @SuppressWarnings("unused")
    public void setTransferable(String transferable) {
        setParameter("transferableExpr", transferable);
    }

    /**
     * Get the transferable property from the related scxml tag
     * @return "true" if transferable, otherwise "false"
     */
    public String getTransferable() {
        return getParameter("transferableExpr");
    }

    @Override
    protected SetTransferableTask createWorkflowTask() {
        return new SetTransferableTask();
    }

    @Override
    protected void initTask(final SetTransferableTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        String transferable = getTransferable();
        if (transferable != null) {
            task.setTransferable(eval(getTransferable()));
        }
    }
}
