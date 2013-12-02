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

        if (StringUtils.isBlank(getHint())) {
            throw new ModelException("No hint specified");
        }

        Context ctx = scInstance.getContext(getParentTransitionTarget());

        Serializable attrValue = null;

        if (getValue() != null) {
            attrValue = (Serializable)scInstance.getEvaluator().eval(ctx, getValue());
        }

        DocumentHandle dm = (DocumentHandle)ctx.get("dm");
        if (attrValue == null) {
            dm.getHints().remove(getHint());
        }
        else {
            dm.getHints().put(getHint(), attrValue);
        }
    }
}
