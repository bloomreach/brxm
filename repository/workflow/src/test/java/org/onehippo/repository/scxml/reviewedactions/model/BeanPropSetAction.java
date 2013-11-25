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
package org.onehippo.repository.scxml.reviewedactions.model;

import java.util.Collection;

import org.apache.commons.beanutils.PropertyUtils;
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
 * BeanPropSetAction, only for custom action testing purpose.
 */
public class BeanPropSetAction extends Action {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(BeanPropSetAction.class);

    private String beanExpr;
    private String property;
    private String valueExpr;

    public String getBeanExpr() {
        return beanExpr;
    }

    public void setBeanExpr(String beanExpr) {
        this.beanExpr = beanExpr;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getValueExpr() {
        return valueExpr;
    }

    public void setValueExpr(String valueExpr) {
        this.valueExpr = valueExpr;
    }

    @Override
    public void execute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException {
        try {
            Context ctx = scInstance.getContext(getParentTransitionTarget());
            Object bean = scInstance.getEvaluator().eval(ctx, beanExpr);
            Object value = scInstance.getEvaluator().eval(ctx, valueExpr);
            PropertyUtils.setProperty(bean, property, value);
        } catch (Exception e) {
            log.warn("Failed to set property.", e);
        }
    }
}
