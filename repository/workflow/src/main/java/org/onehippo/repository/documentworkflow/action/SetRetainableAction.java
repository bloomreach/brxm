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
import org.onehippo.repository.documentworkflow.task.SetRetainableTask;
import org.onehippo.repository.documentworkflow.task.SetTransferableTask;

/**
 * SetRetainableAction is a custom DocumentWorkflow SCXML state machine action for setting or removing the
 * {@link org.hippoecm.repository.HippoStdNodeType#HIPPOSTD_RETAINABLE} property of the current
 * draft document variant.
 * <p>
 * The execution of this task is delegated to its corresponding {@link SetTransferableTask}.
 * </p>
 */
public final class SetRetainableAction extends AbstractDocumentTaskAction<SetRetainableTask> {

    private static final long serialVersionUID = 1L;

    /**
     * <p>Reads the retainable property from the scxml retainable tag.</p>
     * @param retainable as read from the matching scxml tag
     */
    @SuppressWarnings("unused")
    public void setRetainable(final String retainable) {
        setParameter("retainableExpr", retainable);
    }

    /**
     * <p>Get the retainable property from the related scxml tag.</p>
     * @return "true" if retainable, otherwise "false"
     */
    public String getRetainable() {
        return getParameter("retainableExpr");
    }

    @Override
    protected SetRetainableTask createWorkflowTask() {
        return new SetRetainableTask();
    }

    @Override
    protected void initTask(final SetRetainableTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        String retainable = getRetainable();
        if (retainable != null) {
            task.setRetainable(eval(getRetainable()));
        }
    }
}
