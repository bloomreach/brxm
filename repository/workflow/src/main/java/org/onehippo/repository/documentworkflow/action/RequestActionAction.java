/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.scxml.AbstractAction;
import org.onehippo.repository.scxml.SCXMLWorkflowContext;

public class RequestActionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public String getIdentifierExpr() {
        return getParameter("identifierExpr");
    }

    @SuppressWarnings("unused")
    public void setIdentifierExpr(String identifierExpr) {
        setParameter("identifierExpr", identifierExpr);
    }

    public String getAction() {
        return getParameter("action");
    }

    @SuppressWarnings("unused")
    public void setAction(final String action) {
        setParameter("action", action);
    }

    public String getEnabledExpr() {
        return getParameter("enabledExpr");
    }

    @SuppressWarnings("unused")
    public void setEnabledExpr(final String enabled) {
        setParameter("enabledExpr", enabled);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doExecute(ActionExecutionContext exctx) throws ModelException, SCXMLExpressionException {

        String identifier = (StringUtils.isBlank(getIdentifierExpr()) ? null : (String)eval(getIdentifierExpr()));
        if (StringUtils.isBlank(identifier)) {
            throw new ModelException("No identifier specified");
        }

        String action = getAction();
        if (StringUtils.isBlank(action)) {
            throw new ModelException("No action specified");
        }

        String enabledExpr = getEnabledExpr();
        Boolean enabled = (StringUtils.isBlank(enabledExpr) ? null : (Boolean)eval(enabledExpr));

        SCXMLWorkflowContext workflowContext = getSCXMLWorkflowContext();
        Map<String, Map<String, Boolean>> requestsActionsMap = (Map<String, Map<String, Boolean>>)workflowContext.getFeedback().get("requests");
        if (requestsActionsMap == null) {
            requestsActionsMap = new HashMap<>();
            workflowContext.getFeedback().put("requests", (Serializable)requestsActionsMap);
        }
        Map<String,Boolean> requestActions = requestsActionsMap.get(identifier);
        if (requestActions == null) {
            requestActions = new HashMap<>();
            requestsActionsMap.put(identifier, requestActions);
        }
        if (enabled == null) {
            requestActions.remove(action);
        } else {
            requestActions.put(action, enabled);
        }
    }
}
