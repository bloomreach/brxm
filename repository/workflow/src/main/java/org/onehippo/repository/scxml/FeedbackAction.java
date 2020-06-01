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

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;

/**
 * FeedbackAction is a basic SCXML state machine custom action to store a specific {@link #setValue(String) value }
 * expression under the configured {@link #setKey(String) key} in the {@link SCXMLWorkflowContext#getFeedback()
 * feedback} map.
 * <p> If the value expression is empty or evaluates to null the configured key is removed from the map. </p>
 */
public class FeedbackAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public String getKey() {
        return getParameter("key");
    }

    @SuppressWarnings("unused")
    public void setKey(final String key) {
        setParameter("key", key);
    }

    @SuppressWarnings("unused")
    public String getValue() {
        return getParameter("valueExpr");
    }

    public void setValue(final String value) {
        setParameter("valueExpr", value);
    }

    @Override
    protected void doExecute(ActionExecutionContext exctx) throws ModelException, SCXMLExpressionException {

        String key = getKey();
        if (StringUtils.isBlank(key)) {
            throw new ModelException("No feedback key specified");
        }

        String valueExpr = getValue();
        Serializable value = (Serializable) (StringUtils.isBlank(valueExpr) ? null : eval(valueExpr));

        if (value == null) {
            getSCXMLWorkflowContext().getFeedback().remove(key);
        } else {
            getSCXMLWorkflowContext().getFeedback().put(key, value);
        }
    }
}
