/**
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
package org.onehippo.repository.documentworkflow.model;

import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.Action;
import org.apache.commons.scxml2.model.ModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VariantInitAction, only for custom action testing purpose.
 */
public class VariantCopyAction extends Action {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(VariantCopyAction.class);

    private String handleExpr = "handle";
    private String source;
    private String target;

    public String getHandleExpr() {
        return handleExpr;
    }

    public void setHandleExpr(String handleExpr) {
        this.handleExpr = handleExpr;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public void execute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException {
        try {
            log.warn("VariantCopyAction("+handleExpr+","+source+","+target+")");
            Context ctx = scInstance.getContext(getParentTransitionTarget());
            Handle handle = (Handle) scInstance.getEvaluator().eval(ctx, handleExpr);

            if (!handle.getVariants().containsKey(target)) {
                Variant variant = new Variant();

                if (handle.getVariants().containsKey(source) && handle.getVariants().get(source).getProperties() != null) {
                    variant.setProperties(new HashMap<String, Object>(handle.getVariants().get(source).getProperties()));
                }

                handle.getVariants().put(target, variant);
            }
        } catch (Exception e) {
            log.warn("Failed to set property.", e);
        }
    }
}
