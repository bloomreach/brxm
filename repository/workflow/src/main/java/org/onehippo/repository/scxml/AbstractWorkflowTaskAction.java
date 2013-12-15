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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractWorkflowTaskAction
 * <p>
 * SCXML base class for {@link WorkflowTask} based actions
 * </p>
 */
public abstract class AbstractWorkflowTaskAction<T extends WorkflowTask> extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(AbstractWorkflowTaskAction.class);

    public AbstractWorkflowTaskAction() {
    }

    /**
     * Can be overriden to initialize the workflow task
     * @param task
     */
    protected void initTask(T task) throws ModelException, SCXMLExpressionException {
    }

    /**
     * Implementation of the real task execution.
     */
    @Override
    protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException,
            WorkflowException, RepositoryException {

        T task = createWorkflowTask();

        initTask(task);

        processTaskResult(task.execute());
    }

    /**
     * Process the task execution result. By default, it stores the result through {@link SCXMLDataModel#setResult(Object)}
     * if it's a non-null value.
     * This method can be overriden to do something else or more.
     * @param taskResult
     */
    protected void processTaskResult(Object taskResult) {
        if (taskResult != null) {
            getDataModel().setResult(taskResult);
        }
    }

    /**
     * Creates a workflow task.
     * This method is invoked to create a new workflow task.
     * @return
     */
    protected abstract T createWorkflowTask();

}
