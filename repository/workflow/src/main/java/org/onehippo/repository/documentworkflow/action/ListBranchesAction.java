/*
 * Copyright 2018-2023 Bloomreach
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
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.task.ListBranchesTask;
import org.onehippo.repository.scxml.SCXMLWorkflowContext;

/**
 * <p>
 *    Returns the available branches available via {@link DocumentHandle#getBranches()} and sets this on the
 *    {@link SCXMLWorkflowContext}. This action is idempotent
 * </p>
 */
public class ListBranchesAction extends AbstractDocumentTaskAction<ListBranchesTask> {

    private static final long serialVersionUID = 1L;

    @Override
    protected ListBranchesTask createWorkflowTask() {
        return new ListBranchesTask();
    }

    @Override
    protected void initTask(ListBranchesTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
    }
}
