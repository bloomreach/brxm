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
import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.Action;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractAction
 * <P>
 * Abstract base class for SCXML Action implementations.
 * </P>
 */
public abstract class AbstractAction extends Action {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(AbstractAction.class);

    private static ThreadLocal<SCInstance> tlSCInstance = new ThreadLocal<SCInstance>();

    @Override
    public final void execute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException {
        try {
            tlSCInstance.set(scInstance);
            doExecute(evtDispatcher, errRep, appLog, derivedEvents);
        } catch (WorkflowException e) {
            throw new ModelException(e);
        } catch (RepositoryException e) {
            throw new ModelException(e);
        } finally {
            tlSCInstance.remove();
        }
    }

    /**
     * An SCXML action implementation should implement this method to include the real execution code.
     * @param evtDispatcher
     * @param errRep
     * @param appLog
     * @param derivedEvents
     * @throws ModelException
     * @throws SCXMLExpressionException
     * @throws WorkflowException
     * @throws RepositoryException
     */
    abstract protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException, WorkflowException, RepositoryException;

    /**
     * Returns the context object by the name.
     * @param scInstance
     * @param name
     * @return
     * @throws org.apache.commons.scxml2.model.ModelException
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextAttribute(String name) {
        try {
            Context ctx = getCurrentSCInstance().getContext(getParentTransitionTarget());
            return (T) ctx.get(name);
        } catch (ModelException e) {
            log.error("Failed to retrieve the parent transition target from the current execution context.", e);
        }

        return null;
    }

    /**
     * Sets a context object attribute by the name
     * @param name
     * @param value
     */
    public void setContextAttribute(String name, Object value) {
        try {
            Context ctx = getCurrentSCInstance().getContext(getParentTransitionTarget());
            ctx.set(name, value);
        } catch (ModelException e) {
            log.error("Failed to retrieve the parent transition target from the current execution context.", e);
        }
    }

    /**
     * Evaluates the expression and returns the last evaluated value.
     * @param scInstance
     * @param expr
     * @return
     * @throws org.apache.commons.scxml2.model.ModelException
     * @throws org.apache.commons.scxml2.SCXMLExpressionException
     */
    @SuppressWarnings("unchecked")
    public <T> T eval(String expr) throws ModelException, SCXMLExpressionException {
        Context ctx = getCurrentSCInstance().getContext(getParentTransitionTarget());
        return (T) getCurrentSCInstance().getEvaluator().eval(ctx, expr);
    }

    private static SCInstance getCurrentSCInstance() {
        final SCInstance scInstance = tlSCInstance.get();

        if (scInstance == null) {
            throw new IllegalStateException("No SCInstance in the current SCXML execution context.");
        }

        return scInstance;
    }

}
