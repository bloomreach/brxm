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

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;

/**
 * ActionAction sets a provided action value in the {@link SCXMLDataModel}, or removes it if empty/null
 */
public class ActionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public String getAction() {
        return getParameter("action");
    }

    public void setAction(final String action) {
        setParameter("action", action);
    }

    public String getValue() {
        return getParameter("valueExpr");
    }

    public void setValue(final String value) {
        setParameter("valueExpr", value);
    }

    @Override
    protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, Log appLog,
                             Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException {

        String action = getAction();
        if (StringUtils.isBlank(action)) {
            throw new ModelException("No action specified");
        }

        String valueExpr = getValue();
        Object value = (StringUtils.isBlank(valueExpr) ? null : eval(valueExpr));
        if (value != null && ! (value instanceof Boolean)) {
            throw new ModelException("Action "+action+" value must be of type boolean. Value: "+value.toString());
        }

        SCXMLDataModel dm = getDataModel();
        if (value == null) {
            dm.getActions().remove(action);
        } else {
            dm.getActions().put(action, (Boolean)value);
        }
    }
}
