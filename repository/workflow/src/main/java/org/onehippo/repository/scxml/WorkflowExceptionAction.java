/*
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowException;

/**
 * WorkflowExceptionAction is a basic SCXML state machine custom action to throw a {@link WorkflowException} with a
 * specific runtime evaluated error message, optionally under a specific runtime evaluated condition.
 */
public class WorkflowExceptionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public String getCond() {
        return getParameter("condExpr");
    }

    /**
     * Sets the optional condition expression which will be evaluated at runtime to determine if the specified
     * workflow exception should be raised.
     * @param condExpr the optional condition expression which if defined should resolve to Boolean.TRUE to trigger
     *                 raising the specified workflow exception
     */
    @SuppressWarnings("unused")
    public void setCond(String condExpr) {
        setParameter("condExpr", condExpr);
    }

    public String getErrorExpr() {
        return getParameter("errorExpr");
    }

    /**
     * Sets the runtime evaluated expression for the error message to be used when raising the {@link WorkflowException}
     * @param errorExpr the error (message) expression which should evaluate to a String
     */
    @SuppressWarnings("unused")
    public void setErrorExpr(final String errorExpr) {
        setParameter("errorExpr", errorExpr);
    }

    @Override
    protected void doExecute(ActionExecutionContext exctx) throws ModelException, SCXMLExpressionException, WorkflowException {

        String condExpr = getCond();
        if (!StringUtils.isBlank(condExpr) && !((Boolean)eval(condExpr))) {
            return;
        }
        String errorExpr = getErrorExpr();
        if (StringUtils.isBlank(errorExpr)) {
            throw new ModelException("No error specified");
        }
        throw new WorkflowException((String)eval(errorExpr));
    }
}
