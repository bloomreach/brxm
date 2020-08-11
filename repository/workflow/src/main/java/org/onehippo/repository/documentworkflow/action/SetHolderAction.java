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
import org.onehippo.repository.documentworkflow.task.SetHolderTask;

/**
 * SetHolderAction is a custom DocumentWorkflow SCXML state machine action for setting or removing the current
 * draft document variant holder.
 * <p>
 * The execution of this task is delegated to its corresponding {@link SetHolderTask}.
 * </p>
 */
public class SetHolderAction extends AbstractDocumentTaskAction<SetHolderTask> {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    public void setHolder(String holder) {
        setParameter("holderExpr", holder);
    }

    public String getHolder() {
        return getParameter("holderExpr");
    }
    @Override
    protected SetHolderTask createWorkflowTask() {
        return new SetHolderTask();
    }

    @Override
    protected void initTask(SetHolderTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        String holder = getHolder();
        if (holder != null) {
            task.setHolder((String) eval(getHolder()));
        }
    }
}
