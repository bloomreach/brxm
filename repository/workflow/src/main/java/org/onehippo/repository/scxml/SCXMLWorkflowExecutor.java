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

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SCXMLWorkflowExecutor wrapping {@link SCXMLExecutor} invocations with extra Workflow related error/exception/state handling
 */
public class SCXMLWorkflowExecutor<T extends SCXMLWorkflowContext, V extends SCXMLWorkflowData> {

    private static final Logger log = LoggerFactory.getLogger(SCXMLWorkflowExecutor.class);

    private final String scxmlId;
    private final SCXMLExecutor executor;
    private final T context;
    private final V data;
    private boolean started;
    private boolean terminated;

    public SCXMLWorkflowExecutor(T context, V data) throws WorkflowException {

        this.context = context;
        this.data = data;
        this.scxmlId = context.getScxmlId();

        SCXMLRegistry scxmlRegistry = HippoServiceRegistry.getService(SCXMLRegistry.class);
        SCXMLDefinition scxmlDef = scxmlRegistry.getSCXMLDefinition(scxmlId);

        if (scxmlDef == null) {
            throw new WorkflowException("SCXML workflow definition "+scxmlId+" not found.");
        }

        SCXMLExecutorFactory scxmlExecutorFactory = HippoServiceRegistry.getService(SCXMLExecutorFactory.class);
        try {
            executor = scxmlExecutorFactory.createSCXMLExecutor(scxmlDef);
        } catch (SCXMLException e) {
            throw new WorkflowException("SCXML workflow executor creation failed", e);
        }
    }

    public SCXMLExecutor getSCXMLExecutor() {
        return executor;
    }

    public T getContext() {
        return context;
    }

    public V getData() {
        return data;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void reset() {
        terminated = false;
        started = false;
        context.reset();
        if (data != null) {
            data.reset();
        }
    }

    protected void handleException(Exception e) throws WorkflowException {
        if (e instanceof WorkflowException) {
            log.error(e.getMessage(), e);
            throw (WorkflowException)e;
        }
        else {
            Throwable logCause = e;
            Exception exCause = e;
            if (e instanceof ModelException) {
                if (e.getCause() != null) {
                    logCause = e.getCause();
                    if (e.getCause() instanceof RepositoryException) {
                        exCause = (RepositoryException)e.getCause();
                    }
                    else if (e.getCause() instanceof WorkflowException) {
                        exCause = (WorkflowException)e.getCause();
                    }
                    else {
                        if (e.getCause() instanceof Exception) {
                            exCause = (Exception)e.getCause();
                        }
                        else {
                            exCause = new Exception(e.getCause());
                        }
                    }
                }
            }
            else if (e instanceof RuntimeException) {
                if (!(e instanceof SCXMLExecutionError)) {
                    throw (RuntimeException)e;
                }
            }
            else {
                throw new RuntimeException(e);
            }
            if (exCause instanceof WorkflowException) {
                log.error(exCause.getMessage(), exCause);
                throw (WorkflowException)exCause;
            }
            log.error("Workflow {} execution failed", scxmlId, logCause);
            throw new WorkflowException("Workflow "+scxmlId+" execution failed", exCause);
        }
    }

    /**
     * Invokes {@link SCXMLExecutor#go()} on the wrapping SCXMLExecutor.
     * @return {@link SCXMLWorkflowContext#getResult()} if there's no exception.
     */
    public Object start() throws WorkflowException {
        if (terminated) {
            throw new WorkflowException("Workflow "+scxmlId+" already terminated");
        }
        context.initialize();
        if (data != null) {
            data.initialize();
        }
        getSCXMLExecutor().getRootContext().set(SCXMLWorkflowContext.SCXML_CONTEXT_KEY, context);
        getSCXMLExecutor().getRootContext().set(SCXMLWorkflowData.SCXML_CONTEXT_KEY, data);
        log.info("Starting workflow {}", scxmlId);
        try {
            executor.go();
            started = true;
            if (executor.getCurrentStatus().isFinal()) {
                terminated = true;
            }
        } catch (Exception e) {
            handleException(e);
        }
        // only reached when no exception
        return context.getResult();
    }

    /**
     * Invokes {@link SCXMLExecutor#triggerEvent(TriggerEvent)} with a {@link TriggerEvent#SIGNAL_EVENT} and the provided action as event name
     * <p>If the triggering of the action is allowed will first be validated against the {@link SCXMLWorkflowContext#getActions()}</p>
     * @return {@link SCXMLWorkflowContext#getResult()} if there's no exception.
     */
    public Object triggerAction(String action) throws WorkflowException {
        return triggerAction(action, context.getActions(), null);
    }

    /**
     * Invokes {@link SCXMLExecutor#triggerEvent(TriggerEvent)} with a {@link TriggerEvent#SIGNAL_EVENT} and the provided action as event name
     * <p>If the triggering of the action is allowed will first be validated against the provided actionsMap}</p>
     * @return {@link SCXMLWorkflowContext#getResult()} if there's no exception.
     */
    @SuppressWarnings("unused")
    public Object triggerAction(String action, Map<String, Boolean> actionsMap) throws WorkflowException {
        return triggerAction(action, actionsMap, null);
    }

    /**
     * Invokes {@link SCXMLExecutor#triggerEvent(TriggerEvent)} with a {@link TriggerEvent#SIGNAL_EVENT}, the provided action as event name and payload as event payload
     * <p>If the triggering of the action is allowed will first be validated against the {@link SCXMLWorkflowContext#getActions()}</p>
     * @return {@link SCXMLWorkflowContext#getResult()} if there's no exception.
     */
    public Object triggerAction(String action, Object payload) throws WorkflowException {
        return triggerAction(action, context.getActions(), payload);
    }

    /**
     * Invokes {@link SCXMLExecutor#triggerEvent(TriggerEvent)} with a {@link TriggerEvent#SIGNAL_EVENT}, the provided action as event name and payload as event payload
     * <p>If the triggering of the action is allowed will first be validated against the provided actionsMap}</p>
     * @return {@link SCXMLWorkflowContext#getResult()} if there's no exception.
     */
    public Object triggerAction(String action, Map<String, Boolean> actionsMap, Object payload) throws WorkflowException {
        if (!started) {
            throw new WorkflowException("Workflow "+scxmlId+" not started");
        }
        if (terminated) {
            throw new WorkflowException("Workflow "+scxmlId+" already terminated");
        }
        try {
            Boolean allowed = actionsMap.get(action);
            if (allowed == null || !allowed) {
                throw new WorkflowException("Cannot invoke workflow "+scxmlId+" action "+action+": action not allowed or undefined");
            }
            TriggerEvent event = new TriggerEvent(action, TriggerEvent.SIGNAL_EVENT, payload);
            if (payload == null) {
                log.info("Invoking workflow {} action {}", scxmlId, action);
            }
            else {
                log.info("Invoking workflow {} action {} with payload {}", new Object[]{scxmlId, action, payload.toString()});
            }
            // reset result
            context.setResult(null);
            executor.triggerEvent(event);
            if (executor.getCurrentStatus().isFinal()) {
                terminated = true;
            }
        } catch (Exception e) {
            handleException(e);
        }
        // only reached when no exception
        return context.getResult();
    }
}