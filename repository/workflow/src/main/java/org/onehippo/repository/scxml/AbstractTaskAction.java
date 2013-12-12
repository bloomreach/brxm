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
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.api.WorkflowTask;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;
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

    private Map<String, String> propertiesMap;
    private Map<String, Object> runtimePropertiesMap;

    public AbstractTaskAction() {
    }

    /**
     * Can be overriden to initialize the workflow task by given properties. The <code>propertiesMap</code> contains
     * all the attribute values which are not evaluated yet in the execution runtime.
     * @param task
     * @param propertiesMap
     */
    protected void initTaskBeforeEvaluation(T task, Map<String, String> propertiesMap) {
        if (task instanceof AbstractDocumentTask) {
            ((AbstractDocumentTask) task).setWorkflowContext((WorkflowContext) getContextAttribute("workflowContext"));
            DocumentHandle dm = getContextAttribute("dm");
            ((AbstractDocumentTask) task).setDataModel(dm);
        }
    }

    /**
     * Can be overriden to initialize the workflow task by given runtime properties. The <code>runtimePropertiesMap</code> contains
     * all the attribute values which were evaluated in the execution runtime.
     * @param task
     * @param runtimePropertiesMap
     */
    protected void initTaskAfterEvaluation(T task, Map<String, Object> runtimePropertiesMap) {
    }

    /**
     * Implementation of the real task execution. This method simply gets the task from {@link #getWorkflowTask()}
     * to invoke the task.
     */
    @Override
    protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException,
            WorkflowException, RepositoryException {

        T task = createWorkflowTask();

        initTaskBeforeEvaluation(task, propertiesMap);
        Map<String, Object> runtimePropertiesMap = getEvaluatedRuntimePropertiesMap();
        initTaskAfterEvaluation(task, runtimePropertiesMap);

        Object eventResult = task.execute();

        if (eventResult != null) {
            setContextAttribute("eventResult", eventResult);
        }
    }

    private Map<String, Object> getEvaluatedRuntimePropertiesMap() {
        Map<String, Object> runtimePropertiesMap = new HashMap<String, Object>(getRuntimePropertiesMap());

        for (Map.Entry<String, Object> entry : runtimePropertiesMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value != null && value instanceof String) {
                try {
                    Object runtimeValue = eval((String) value);
                    runtimePropertiesMap.put(key, runtimeValue);
                } catch (Exception e) {
                    log.error("Failed to evaluate dynamic property expression in executing " + getClass().getName() + ": '" + value + "'.", e);
                }
            }
        }

        return runtimePropertiesMap;
    }

    /**
     * Returns the properties map. If not exists, it creates a new map first.
     * @return
     */
    protected Map<String, String> getPropertiesMap() {
        if (propertiesMap == null) {
            propertiesMap = new HashMap<String, String>();
        }

        return propertiesMap;
    }

    /**
     * Returns the runtime properties map. If not exists, it creates a new map first.
     * @return
     */
    protected Map<String, Object> getRuntimePropertiesMap() {
        if (runtimePropertiesMap == null) {
            runtimePropertiesMap = new HashMap<String, Object>();
        }

        return runtimePropertiesMap;
    }

    /**
     * Creates a workflow task.
     * This method is invoked to create a new workflow task.
     * @return
     */
    protected abstract T createWorkflowTask();

}
