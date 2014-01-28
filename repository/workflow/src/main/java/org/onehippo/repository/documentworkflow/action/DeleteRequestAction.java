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
import org.onehippo.repository.documentworkflow.task.DeleteRequestTask;

/**
 * DeleteRequestAction delegating the execution to DeleteRequestTask.
 */
public class DeleteRequestAction extends AbstractDocumentTaskAction<DeleteRequestTask> {

    private static final long serialVersionUID = 1L;

    public void SetIdentity(String identity) {
        setParameter("identityExpr", identity);
    }

    public String getIdentity() {
        return getParameter("identityExpr");
    }

    @Override
    protected DeleteRequestTask createWorkflowTask() {
        return new DeleteRequestTask();
    }

    @Override
    protected void initTask(DeleteRequestTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        String identity = getIdentity();
        if (identity != null) {
            task.setIdentity((String) eval(getIdentity()));
        }
    }
}
