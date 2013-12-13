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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.Context;
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
 * <p>
 * SCXML base class for {@link WorkflowTask} based actions
 * </p>
 */
public abstract class AbstractWorkflowTaskAction<T extends WorkflowTask> extends AbstractAction {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(AbstractWorkflowTaskAction.class);

    private Map<String, String> parameters;

    public AbstractWorkflowTaskAction() {
    }

    @Override
    protected void goImmutable() {
        if (!isImmutable()) {
            if (parameters == null) {
                parameters = Collections.EMPTY_MAP;
            }
            else {
                parameters = Collections.unmodifiableMap(parameters);
            }
        }
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

        processTaskResult(getContext(), task.execute());
    }

    protected void processTaskResult(Context context, Object taskResult) {
        if (taskResult != null) {
            context.set("eventResult", taskResult);
        }
    }

    /**
     * Returns the properties map. If not exists, it creates a new map first.
     * @return
     */
    protected Map<String, String> getParameters() {
        if (parameters == null) {
            parameters = new HashMap<>();
        }

        return parameters;
    }

    protected String getParameter(String name) {
        return getParameters().get(name);
    }

    protected String getParameter(String name, String defaultValue) {
        String value = getParameters().get(name);
        return value != null ? value : defaultValue;
    }

    protected String setParameter(String name, String value) {
        return getParameters().put(name, value);
    }

    /**
     * Creates a workflow task.
     * This method is invoked to create a new workflow task.
     * @return
     */
    protected abstract T createWorkflowTask();

}
