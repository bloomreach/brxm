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

/**
 * ActionAction is a basic SCXML state machine custom action to store a specific {@link #setAction(String) action} as
 * enabled in the {@link SCXMLWorkflowContext#getActions() map}  if the configured {@link #setEnabledExpr(String)
 * expression} evaluates at runtime to Boolean.TRUE, or else removes it from the map.
 */
public class ActionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public String getAction() {
        return getParameter("action");
    }

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
    protected void doExecute(ActionExecutionContext exctx) throws ModelException, SCXMLExpressionException {

        String action = getAction();
        if (StringUtils.isBlank(action)) {
            throw new ModelException("No action specified");
        }

        String enabledExpr = getEnabledExpr();
        Boolean enabled = (StringUtils.isBlank(enabledExpr) ? null : (Boolean) eval(enabledExpr));

        if (enabled == null) {
            getSCXMLWorkflowContext().getActions().remove(action);
        } else {
            getSCXMLWorkflowContext().getActions().put(action, enabled);
        }
    }
}
