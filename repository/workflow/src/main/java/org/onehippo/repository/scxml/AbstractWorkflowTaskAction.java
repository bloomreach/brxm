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
package org.onehippo.repository.scxml;

import javax.jcr.RepositoryException;

import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.api.WorkflowTask;

/**
 * AbstractWorkflowTaskAction provides a base class for custom SCXML state machine actions which involve executing
 * a specific {@link WorkflowTask}.
 * <p>
 * Such {@link WorkflowTask}s represent units of work to be executed independent of SCXML workflow execution
 * context following the command pattern.
 * </p>
 * <p>
 * This base class automatically {@link #doExecute(ActionExecutionContext) executes} such a task and provides
 * default {@link #processTaskResult(Object) processing} of the result, leaving only the implementation of
 * {@link #createWorkflowTask() creating} and {@link #initTask(WorkflowTask) initializing} of the task to the
 * concrete implementation of this base class.
 * </p>
 */
public abstract class AbstractWorkflowTaskAction<T extends WorkflowTask> extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public AbstractWorkflowTaskAction() {
    }

    /**
     * Can be overridden to initialize the workflow task
     */
    protected void initTask(T task) throws ModelException, SCXMLExpressionException {
    }

    /**
     * Implementation of the real task execution.
     */
    @Override
    protected void doExecute(ActionExecutionContext exctx) throws ModelException, SCXMLExpressionException,
            WorkflowException, RepositoryException {

        T task = createWorkflowTask();

        initTask(task);

        processTaskResult(task.execute());
    }

    /**
     * Process the task execution result. By default, it stores the result through {@link SCXMLWorkflowContext#setResult(Object)}
     * if it's a non-null value.
     * This method can be overridden to do something else or more.
     */
    protected void processTaskResult(Object taskResult) {
        if (taskResult != null) {
            getSCXMLWorkflowContext().setResult(taskResult);
        }
    }

    /**
     * Creates a workflow task.
     * This method is invoked to create a new workflow task.
     */
    protected abstract T createWorkflowTask();
}
