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
import java.util.HashMap;
import java.util.Map;

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
 * AbstractTaskAction
 * <P>
 * Delegating SCXML Action abstract base class.
 * </P>
 */
public abstract class AbstractTaskAction<T extends WorkflowTask> extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(AbstractTaskAction.class);

    private T workflowTask;
    private Map<String, Object> properties;

    public AbstractTaskAction() {
    }

    /**
     * Can be overriden to initialize the workflow task by given properties. The <code>properties</code> contains
     * all the attribute values which are not evaluated yet in the execution runtime.
     * @param properties
     */
    protected void initTaskBeforeEvaluation(Map<String, Object> properties) {
    }

    /**
     * Can be overriden to initialize the workflow task by given dynamic properties. The <code>dynamicProperties</code> contains
     * all the attribute values which were evaluated in the execution runtime.
     * @param properties
     */
    protected void initTaskAfterEvaluation(Map<String, Object> properties) {
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

        initTaskBeforeEvaluation(properties);
        evaluateProperties();
        initTaskAfterEvaluation(properties);

        task.execute(properties);

    }

    private void evaluateProperties() {
        for (Map.Entry<String, Object> entry : getProperties().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value != null && value instanceof String) {
                try {
                    getProperties().put(key, eval((String) value));
                } catch (Exception e) {
                    log.error("Failed to evaluate dynamic property expression, '" + value + "'.", e);
                }
            }
        }
    }

    /**
     * Returns the properties map. If not exists, it creates a new map first.
     * @return
     */
    protected Map<String, Object> getProperties() {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }

        return properties;
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
        }

        return workflowTask;
    }

    /**
     * Sets the current associated workflow task.
     * @param workflowTask
     */
    protected void setWorkflowTask(T workflowTask) {
        this.workflowTask = workflowTask;
    }

    /**
     * Creates a workflow task.
     * This method is invoked to create a new workflow task.
     * @return
     */
    protected abstract T createWorkflowTask();

}
