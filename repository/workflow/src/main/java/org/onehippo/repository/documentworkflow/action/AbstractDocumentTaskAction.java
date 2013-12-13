/**
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

import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowContext;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;
import org.onehippo.repository.scxml.AbstractWorkflowTaskAction;

/**
 * AbstractDocumentTaskAction
 * <p>
 * SCXML base class for {@link AbstractDocumentTask} based actions
 * </p>
 */
public abstract class AbstractDocumentTaskAction<T extends AbstractDocumentTask> extends AbstractWorkflowTaskAction<T> {

    @Override
    protected void initTask(T task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);

        Context ctx = getContext();
        task.setWorkflowContext((WorkflowContext) ctx.get("workflowContext"));
        task.setDataModel((DocumentHandle)ctx.get("dm"));
    }
}
