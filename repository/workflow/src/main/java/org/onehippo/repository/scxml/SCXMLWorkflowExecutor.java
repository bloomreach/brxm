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

import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.apache.commons.scxml2.model.TransitionTarget;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SCXMLWorkflowExecutor wrapping {@link SCXMLExecutor} invocations with extra Workflow related error/exception/state handling
 */
public class SCXMLWorkflowExecutor {

    private static final Logger log = LoggerFactory.getLogger(SCXMLWorkflowExecutor.class);

    private final String scxmlId;
    private final SCXMLExecutor executor;
    private SCXMLDataModel dm;
    private boolean resetRequired = true;
    private boolean terminated;

    public SCXMLWorkflowExecutor(final String scxmlId, SCXMLDataModel dm) throws WorkflowException {

        this.scxmlId = scxmlId;

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

        setDataModel(dm);
    }

    public SCXMLExecutor getSCXMLExecutor() {
        return executor;
    }

    public SCXMLDataModel getDataModel() {
        return dm;
    }

    public void setDataModel(SCXMLDataModel dm) {
        this.dm = dm;
        reset();
    }

    public boolean isStarted() {
        return !resetRequired && !terminated;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void reset() {
        terminated = false;
        resetRequired = true;
        prepare();
    }

    protected void prepare() {
        getSCXMLExecutor().getRootContext().set(SCXMLDataModel.CONTEXT_KEY, getDataModel());
        if (resetRequired) {
            getDataModel().reset();
        }
        getDataModel().setResult(null);
    }

    protected void checkFinalState() {
        resetRequired = false;
        if (executor.getCurrentStatus().isFinal()) {
            Set<TransitionTarget> targets = executor.getCurrentStatus().getStates();
            for (TransitionTarget target : targets) {
                if (SCXMLDataModel.FINAL_RESET_STATE_ID.equals(target.getId())) {
                    resetRequired = true;
                }
            }
            if (resetRequired) {
                getDataModel().reset();
            }
            else {
                terminated = true;
            }
        }
    }

    protected void handleException(Exception e, boolean noResetRequired) throws WorkflowException {
        if (!noResetRequired) {
            resetRequired = true;
            getDataModel().reset();
        }
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
     * @return {@link SCXMLDataModel#getResult()} if there's no exception.
     */
    public Object start() throws WorkflowException {
        if (terminated) {
            throw new WorkflowException("Workflow "+scxmlId+" already terminated");
        }
        prepare();
        resetRequired = false;
        log.info("Starting workflow {}", scxmlId);
        try {
            executor.go();
            checkFinalState();
        } catch (Exception e) {
            handleException(e, false);
        }
        // only reached when no exception
        return getDataModel().getResult();
    }

    /**
     * Invokes {@link SCXMLExecutor#triggerEvent(TriggerEvent)} with a {@link TriggerEvent#SIGNAL_EVENT} and the provided action as event name
     * @return {@link SCXMLDataModel#getResult()} if there's no exception.
     */
    public Object triggerAction(String action) throws WorkflowException {
        return triggerAction(action, null);
    }

    /**
     * Invokes {@link SCXMLExecutor#triggerEvent(TriggerEvent)} with a {@link TriggerEvent#SIGNAL_EVENT}, the provided action as event name and payload as event payload
     * @return {@link SCXMLDataModel#getResult()} if there's no exception.
     */
    public Object triggerAction(String action, Object payload) throws WorkflowException {
        boolean noResetRequired = false;
        if (terminated) {
            throw new WorkflowException("Workflow "+scxmlId+" already terminated");
        }
        try {
            if (resetRequired) {
                log.info("Resetting workflow {}", scxmlId);
                prepare();
                executor.go();
                checkFinalState();
                if (terminated || resetRequired) {
                    throw new WorkflowException("Cannot invoke workflow "+scxmlId+" action "+action+": workflow already terminated or reset");
                }
            }
            Boolean allowed = getDataModel().getActions().get(action);
            if (allowed == null || !allowed) {
                noResetRequired = true;
                throw new WorkflowException("Cannot invoke workflow "+scxmlId+" action "+action+": action not allowed or undefined");
            }
            TriggerEvent event = new TriggerEvent(action, TriggerEvent.SIGNAL_EVENT, payload);
            if (payload == null) {
                log.info("Invoking workflow {} action {}", scxmlId, action);
            }
            else {
                log.info("Invoking workflow {} action {} with payload {}", new Object[]{scxmlId, action, payload.toString()});
            }
            prepare();
            executor.triggerEvent(event);
            checkFinalState();
        } catch (Exception e) {
            handleException(e, noResetRequired);
        }
        // only reached when no exception
        return getDataModel().getResult();
    }
}
