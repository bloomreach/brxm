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

package org.onehippo.repository.documentworkflow;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.Action;
import org.apache.commons.scxml2.model.ModelException;

/**
 * HintAction sets or removes a DocumentHandle (dm context variable) hints key
 */
public class HintAction extends Action {

    private static final long serialVersionUID = 1L;

    private String hint;
    private String value;

    /**
     * Returns the context object by the name.
     * @param scInstance
     * @param name
     * @return
     * @throws org.apache.commons.scxml2.model.ModelException
     */
    @SuppressWarnings("unchecked")
    protected <T> T getContextAttribute(SCInstance scInstance, String name) throws ModelException {
        Context ctx = scInstance.getContext(getParentTransitionTarget());
        return (T) ctx.get(name);
    }

    /**
     * Evaluates the expression and returns the last evaluated value.
     * @param scInstance
     * @param expr
     * @return
     * @throws org.apache.commons.scxml2.model.ModelException
     * @throws org.apache.commons.scxml2.SCXMLExpressionException
     */
    @SuppressWarnings("unchecked")
    protected <T> T eval(SCInstance scInstance, String expr) throws ModelException, SCXMLExpressionException {
        Context ctx = scInstance.getContext(getParentTransitionTarget());
        return (T) scInstance.getEvaluator().eval(ctx, expr);
    }

    /**
     * Returns the document handle object from the current SCXML execution context.
     * @param scInstance
     * @return
     * @throws org.apache.commons.scxml2.model.ModelException
     * @throws org.apache.commons.scxml2.SCXMLExpressionException
     */
    protected DocumentHandle getDataModel(SCInstance scInstance) throws ModelException {
        return getContextAttribute(scInstance, "dm");
    }

    public String getHint() {
        return hint;
    }

    public void setHint(final String hint) {
        this.hint = hint;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public final void execute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
                              Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException {

        Serializable attrValue = null;

        if (getHint() != null) {

            if (getValue() != null) {
                attrValue = eval(scInstance, getValue());
            }
            if (StringUtils.isBlank(getHint())) {
                throw new ModelException("No hint specified");
            }

            DocumentHandle dm = getDataModel(scInstance);

            if (attrValue == null || (attrValue instanceof String && StringUtils.isBlank((String)attrValue))) {
                dm.getHints().remove(getHint());
            }
            else {
                if (attrValue instanceof String && ("true".equals(attrValue) || "false".equals(attrValue))) {
                    attrValue = Boolean.valueOf((String)attrValue);
                }
                dm.getHints().put(getHint(), attrValue);
            }
        }
    }
}
