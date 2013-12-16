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

package org.onehippo.repository.documentworkflow.action;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.scxml.AbstractAction;

/**
 * WorkflowExceptionAction raises a WorkflowException with specified error message optionally under a specific condition
 */
public class WorkflowExceptionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public String getCond() {
        return getParameter("condExpr");
    }

    public void setCond(String condExpr) {
        setParameter("condExpr", condExpr);
    }
    public String getErrorExpr() {
        return getParameter("errorExpr");
    }

    public void setErrorExpr(final String errorExpr) {
        setParameter("errorExpr", errorExpr);
    }

    @Override
    protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, Log appLog,
                             Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException, WorkflowException {

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
