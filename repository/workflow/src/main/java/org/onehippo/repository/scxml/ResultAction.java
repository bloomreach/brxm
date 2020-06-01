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
 * ResultAction is a basic SCXML state machine custom action to store an (SCXML) runtime expression result value as
 * result in the {@link SCXMLWorkflowContext#getResult()} object.
 */
public class ResultAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public String getValue() {
        return getParameter("valueExpr");
    }

    public void setValue(final String value) {
        setParameter("valueExpr", value);
    }

    @Override
    protected void doExecute(ActionExecutionContext exctx) throws ModelException, SCXMLExpressionException {

        String valueExpr = getValue();
        Object value = StringUtils.isBlank(valueExpr) ? null : eval(valueExpr);
        getSCXMLWorkflowContext().setResult(value);
    }
}
