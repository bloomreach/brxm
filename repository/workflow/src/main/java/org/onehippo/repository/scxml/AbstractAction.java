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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.Action;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowException;

/**
 * AbstractAction is a base class Apache Commons SCXML state machine custom Action implementations with specific
 * support for usage with the {@link SCXMLWorkflowExecutor} engine.
 * <p>
 * This base custom action provides access to the {@link SCXMLWorkflowContext} and {@link SCXMLWorkflowData} objects
 * in the SCXML state machine root context which provide the bridge to the invoking workflow implementation.
 * </p>
 * <p>
 * In addition, this base class provides convenience handling of custom Action parameters (defined as SCXML element
 * attributes) which can be used concurrently as reusable and thus immutable parameters between different SCXML state
 * machine instance executions. The underlying parameter map will be 'locked down' and made immutable on the first
 * execution of this action instance, to enforce this restricted usage.
 * </p>
 */
public abstract class AbstractAction extends Action {

    private static final long serialVersionUID = 1L;

    private static ThreadLocal<ActionExecutionContext> tlExCtx = new ThreadLocal<>();
    private static ThreadLocal<Context> tlContext = new ThreadLocal<>();

    private Map<String, String> parameters;

    private boolean immutable = false;

    @Override
    public final void execute(ActionExecutionContext exctx) throws ModelException, SCXMLExpressionException {
        try {
            tlExCtx.set(exctx);
            if (getContext() == null) {
                tlContext.set(exctx.getContext(getParentEnterableState()));
            }
            synchronized (this) {
                if (!isImmutable()) {
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
            doExecute(exctx);
        } catch (WorkflowException | RepositoryException e) {
            throw new ModelException(e);
        } finally {
            tlContext.remove();
            tlExCtx.remove();
        }
    }

    /**
     * @return true once {@link #doExecute(ActionExecutionContext)}
     * for this instance is about to be invoked for the first time, after which no instance state should be mutable anymore
     */
    protected boolean isImmutable() {
        return immutable;
    }

    /**
     * Called before {@link #doExecute(ActionExecutionContext)} is to be invoked for the first time.
     * {@link Action} instance state must be immutable from that moment on,  as they may be executed concurrently from
     * different threads, and even multiple times within one thread (but only sequentially).
     */
    protected void goImmutable() {}


    /**
     * Returns the properties map. If not exists, it creates a new map first.
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
     * {@link #doExecute(ActionExecutionContext)}
     */
    protected Context getContext() {
        return tlContext.get();
    }

    /**
     * @return the {@link SCXMLWorkflowData} from the {@link Context} of this action.
     * May only be called when invoked within the context of {@link #doExecute(ActionExecutionContext)}
     */
    protected SCXMLWorkflowData getSCXMLWorkflowData() {
        Context context = tlContext.get();
        return context != null ? (SCXMLWorkflowData)context.get(SCXMLWorkflowData.SCXML_CONTEXT_KEY) : null;
    }

    /**
     * @return the {@link SCXMLWorkflowContext} from the {@link Context} of this action.
     * May only be called when invoked within the context of {@link #doExecute(ActionExecutionContext)}
     */
    protected SCXMLWorkflowContext getSCXMLWorkflowContext() {
        Context context = tlContext.get();
        return context != null ? (SCXMLWorkflowContext)context.get(SCXMLWorkflowContext.SCXML_CONTEXT_KEY) : null;
    }

    /**
     * Evaluates the expression by the {@link org.apache.commons.scxml2.Evaluator} using the current
     * {@link #getContext()} and returns the evaluation result.
     * May only be invoked within the context of {@link #doExecute(ActionExecutionContext)}
     * @throws org.apache.commons.scxml2.model.ModelException
     * @throws org.apache.commons.scxml2.SCXMLExpressionException
     */
    @SuppressWarnings("unchecked")
    protected <T> T eval(String expr) throws ModelException, SCXMLExpressionException {
        Context ctx = tlContext.get();
        return (T) tlExCtx.get().getEvaluator().eval(ctx, expr);
    }

    /**
     * An SCXML action implementation should implement this method to include the real execution code.
     */
    abstract protected void doExecute(ActionExecutionContext exctx) throws ModelException,
            SCXMLExpressionException, WorkflowException, RepositoryException;
}
