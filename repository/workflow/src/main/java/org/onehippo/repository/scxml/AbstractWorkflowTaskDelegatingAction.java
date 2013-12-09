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

import java.util.Collection;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.api.WorkflowTask;

/**
 * AbstractWorkflowTaskDelegatingAction
 * <P>
 * Delegating SCXML Action abstract base class.
 * </P>
 */
public abstract class AbstractWorkflowTaskDelegatingAction<T extends WorkflowTask> extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private T workflowTask;

    public AbstractWorkflowTaskDelegatingAction() {
    }

    /**
     * Implementation of the real task execution. This method simply gets the task from {@link #getWorkflowTask()}
     * to invoke the task.
     */
    @Override
    protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException,
            WorkflowException, RepositoryException {

        T task = getWorkflowTask();
        task.execute();

    }

    /**
     * Returns the current associated workflow task.
     * By default, it invokes {@link #createWorkflowTask()} to create a new instance
     * if the task hasn't been created yet.
     * @return
     */
    protected T getWorkflowTask() {
        if (workflowTask == null) {
            workflowTask = createWorkflowTask();

            if (workflowTask instanceof AbstractActionAware) {
                ((AbstractActionAware) workflowTask).setAbstractAction(this);
            }
        }

        return workflowTask;
    }

    /**
     * Creates a workflow task.
     * This method is invoked to create a new workflow task.
     * @return
     */
    protected abstract T createWorkflowTask();

}
