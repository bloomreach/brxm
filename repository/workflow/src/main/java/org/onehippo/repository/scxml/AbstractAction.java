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
    private static ThreadLocal<Context> tlContext = new ThreadLocal<Context>();

    private Map<String, String> parameters;

    private boolean immutable = false;

    @Override
    public final void execute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException {
        try {
            tlSCInstance.set(scInstance);
            if (getContext() == null) {
                tlContext.set(scInstance.getContext(getParentTransitionTarget()));
            }
            synchronized (this) {
                if (!immutable) {
                    goImmutable();

                    if (parameters == null) {
                        parameters = Collections.emptyMap();
                    }
                    else {
                        parameters = Collections.unmodifiableMap(parameters);
                    }

                    immutable = true;
                }
            }
            doExecute(evtDispatcher, errRep, appLog, derivedEvents);
        } catch (WorkflowException e) {
            throw new ModelException(e);
        } catch (RepositoryException e) {
            throw new ModelException(e);
        } finally {
            tlContext.remove();
            tlSCInstance.remove();
        }
    }

    /**
     * @return true once {@link #doExecute(EventDispatcher, ErrorReporter, Log, Collection)}
     * for this instance is about to be invoked for the first time, after which no instance state should be mutable anymore
     */
    protected boolean isImmutable() {
        return immutable;
    }

    /**
     * Called before {@link #doExecute(EventDispatcher, ErrorReporter, Log, Collection)} is to be invoked for the first time.
     * {@link Action} instance state must be immutable from that moment on,  as they may be executed concurrently from
     * different threads, and even multiple times within one thread (but only sequentially).
     */
    protected void goImmutable() {}


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
     * @return the current {@link Context} of this action. Only not null when invoked within the context of
     * {@link #doExecute(EventDispatcher, ErrorReporter, Log, Collection)}
     */
    protected Context getContext() {
        return tlContext.get();
    }

    /**
     * @return the {@link SCXMLDataModel} from the {@link Context} of this action.
     * May only be called when invoked within the context of {@link #doExecute(EventDispatcher, ErrorReporter, Log, Collection)}
     */
    protected SCXMLDataModel getDataModel() {
        Context context = tlContext.get();
        return context != null ? (SCXMLDataModel)context.get(SCXMLDataModel.CONTEXT_KEY) : null;
    }

    /**
     * Evaluates the expression by the {@link org.apache.commons.scxml2.Evaluator} using the current {@link #getContext()} and returns the evaluation result.
     * May only be invoked within the context of {@link #doExecute(EventDispatcher, ErrorReporter, Log, Collection)}
     * @param expr
     * @return
     * @throws org.apache.commons.scxml2.model.ModelException
     * @throws org.apache.commons.scxml2.SCXMLExpressionException
     */
    @SuppressWarnings("unchecked")
    protected <T> T eval(String expr) throws ModelException, SCXMLExpressionException {
        Context ctx = tlContext.get();
        return (T) tlSCInstance.get().getEvaluator().eval(ctx, expr);
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
}
