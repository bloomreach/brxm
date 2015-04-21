/**
 * Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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
 * The SCXMLWorkflowExecutor manages a specific SCXML state machine for workflow state management and processing.
 * <p>
 * The SCXMLWorkflowExecutor uses a dedicated {@link SCXMLWorkflowContext}, which provides the unique SCXML state
 * machine id to be loaded from the repository using the {@link SCXMLRegistry}.
 * </p>
 * <p>
 * The SCXML state machine management itself is delegated to the internal Apache Commons {@link SCXMLExecutor}, and the
 * {@link SCXMLWorkflowContext} and an optional {@link SCXMLWorkflowData} object are injected in the SCXML state machine
 * root context to provide the bridge between the state machine and the invoking workflow.
 * </p>
 * <p>
 * Note that the optional {@link SCXMLWorkflowData} isn't used or needed by the SCXMLWorkflowExecutor itself, other than
 * that when it is provided its initialize and reset methods will be invoked when the state machine is started or reset.
 * </p>
 * <p>
 * The {@link SCXMLWorkflowContext#getActions()} will always be evaluated (or optionally a custom map of
 * allowable actions) first before an action is actually triggered on the SCXML state machine itself.
 * </p>
 * <p>
 * Any exception encountered while executing the state machine is trapped and possibly unwrapped first before being
 * rethrown as a WorkflowException or a RuntimeException if otherwise unknown.
 * </p>
 */
public class SCXMLWorkflowExecutor<T extends SCXMLWorkflowContext, V extends SCXMLWorkflowData> {

    private static final Logger log = LoggerFactory.getLogger(SCXMLWorkflowExecutor.class);

    private final String scxmlId;
    private final SCXMLExecutor executor;
    private final T context;
    private final V data;
    private boolean started;
    private boolean terminated;

    /**
     * Create a new specific SCXMLWorkflowContext using either a standard {@link SCXMLWorkflowContext} or a specialized
     * variant, and optionally a {@link SCXMLWorkflowData} implementation
     * @param context the context providing the {@link SCXMLWorkflowContext#getScxmlId()} and workflow context for the
     *                SCXML state machine and the communication bridge with the invoking workflow implementation
     * @param data optional extra workflow data object, specific to the configured SCXML state machine
     * @throws WorkflowException thrown if the SCXML state machine failed to be loaded and created
     */
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

    /**
     * @return the internal Apache Commons SCXMLExecutor managing the SCXML state machine
     */
    public SCXMLExecutor getSCXMLExecutor() {
        return executor;
    }

    /**
     * @return the SCXML workflow context used for creating this SCXMLWorkflowExecutor
     */
    public T getContext() {
        return context;
    }

    /**
     * @return the optional SCXML workflow data used for creating this SCXMLWorkflowExecutor
     */
    public V getData() {
        return data;
    }

    /**
     * @return true if the SCXML state machine has been initialized and started
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * @return true if the SCXML state machine has been terminated (ended in a final state)
     */
    public boolean isTerminated() {
        return terminated;
    }

    /**
     * Resets the SCXML state machine for re-use, clearing both the started and terminated indicators
     * and resets the {@link SCXMLWorkflowContext} and {@link SCXMLWorkflowData} (if provided).
     */
    public void reset() {
        terminated = false;
        started = false;
        context.reset();
        if (data != null) {
            data.reset();
        }
    }

    /**
     * Unwraps the exception thrown during SCXML state machine execution
     *
     * @param e the encountered exception
     * @throws WorkflowException
     */
    protected void handleException(Exception e) throws WorkflowException {
        if (e instanceof WorkflowException) {
            if (log.isDebugEnabled()) {
                log.warn(e.getMessage(), e);
            } else {
                log.warn(e.getMessage());
            }
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
                if (log.isDebugEnabled()) {
                    log.warn(exCause.getMessage(), exCause);
                } else {
                    log.warn(exCause.getMessage());
                }
                throw (WorkflowException)exCause;
            }
            if (log.isDebugEnabled()) {
                log.warn("Workflow {} execution failed", scxmlId, logCause);
            } else {
                log.warn("Workflow {} execution failed: {}", scxmlId, logCause.getMessage());
            }
            throw new WorkflowException("Workflow "+scxmlId+" execution failed", exCause);
        }
    }

    /**
     * Starts or re-starts the SCXML state machine through {@link SCXMLExecutor#go}.
     * <p>
     * Starting the state machine is not possible if the state machine is terminated.
     * That would require a {@link #reset()} first.
     * </p>
     * <p>
     * After starting the state machine, it <em>might</em> be {@link #isTerminated()} already!
     * </p>
     * <p>
     * Optionally, the state machine might have set a result in the {@link SCXMLWorkflowContext#getResult()}, which
     * for convenience is also returned directly by this method.
     * </p>
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
        log.debug("Starting workflow {}", scxmlId);
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
     * Invokes {@link SCXMLExecutor#triggerEvent(TriggerEvent)} with a {@link TriggerEvent#SIGNAL_EVENT} and the
     * provided action as event name.
     *
     * <p>
     * The action will first be validated against this executor its {@link SCXMLWorkflowContext#getActions()} which must
     * have this action defined with value Boolean.TRUE, otherwise a WorkflowException is thrown before even invoking
     * the state machine.
     * </p>
     * @return {@link SCXMLWorkflowContext#getResult()} if there's no exception.
     */
    public Object triggerAction(String action) throws WorkflowException {
        return triggerAction(action, context.getActions(), null);
    }

    /**
     * Invokes {@link SCXMLExecutor#triggerEvent(TriggerEvent)} with a {@link TriggerEvent#SIGNAL_EVENT}, the provided
     * action as event name and payload as event payload.
     *
     * <p>
     * The action will first be validated against this executor its {@link SCXMLWorkflowContext#getActions()} which must
     * have this action defined with value Boolean.TRUE, otherwise a WorkflowException is thrown before even invoking
     * the state machine.
     * </p>
     * @return {@link SCXMLWorkflowContext#getResult()} if there's no exception.
     */
    public Object triggerAction(String action, Map<String, Object> payload) throws WorkflowException {
        return triggerAction(action, context.getActions(), payload);
    }

    /**
     * Invokes {@link SCXMLExecutor#triggerEvent(TriggerEvent)} with a {@link TriggerEvent#SIGNAL_EVENT}, the provided
     * action as event name and payload as event payload
     * <p>
     * The action will first be validated against the provided custom actions map which must
     * have this action defined with value Boolean.TRUE, otherwise a WorkflowException is thrown before even invoking
     * the state machine.
     * </p>
     * @return {@link SCXMLWorkflowContext#getResult()} if there's no exception.
     */
    public Object triggerAction(String action, Map<String, Boolean> actionsMap, Map<String, Object> payload)
            throws WorkflowException {
        if (!started) {
            throw new WorkflowException("Workflow "+scxmlId+" not started");
        }
        if (terminated) {
            throw new WorkflowException("Workflow "+scxmlId+" already terminated");
        }
        try {
            Boolean allowed = actionsMap.get(action);
            if (allowed == null || !allowed) {
                throw new WorkflowException("Cannot invoke workflow "+ scxmlId +
                        " action "+action+": action not allowed or undefined");
            }
            TriggerEvent event = new TriggerEvent(action, TriggerEvent.SIGNAL_EVENT, payload);
            if (payload == null) {
                log.debug("Invoking workflow {} action {}", scxmlId, action);
            }
            else {
                log.debug("Invoking workflow {} action {} with payload {}", scxmlId, action, payload.toString());
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
