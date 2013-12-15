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

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.scxml.AbstractAction;

/**
 * InfoAction stores a provided info value in the DocumentHandle
 */
public class InfoAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public String getInfo() {
        return getParameter("info");
    }

    public void setInfo(final String info) {
        setParameter("info", info);
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

        String info = getInfo();
        if (StringUtils.isBlank(info)) {
            throw new ModelException("No info specified");
        }

        DocumentHandle dm = (DocumentHandle)getDataModel();

        String valueExpr = getValue();
        Serializable value = (Serializable)(StringUtils.isBlank(valueExpr) ? null : eval(valueExpr));

        if (value == null) {
            dm.getInfo().remove(info);
        } else {
            dm.getInfo().put(info, value);
        }
    }
}
