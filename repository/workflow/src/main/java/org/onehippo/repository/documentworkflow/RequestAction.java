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
package org.onehippo.repository.documentworkflow;

import java.util.Collection;
import java.util.Date;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom action for publication request, depublication request, scheduled publication, scheduled depublication and deletion request.
 */
public class RequestAction extends AbstractDocumentAction {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(RequestAction.class);

    private String type;
    private String contextVariantExpr;
    private String targetDateExpr;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContextVariantExpr() {
        return contextVariantExpr;
    }

    public void setContextVariantExpr(String contextVariantExpr) {
        this.contextVariantExpr = contextVariantExpr;
    }

    public String getTargetDateExpr() {
        return targetDateExpr;
    }

    public void setTargetDateExpr(String targetDateExpr) {
        this.targetDateExpr = targetDateExpr;
    }

    @Override
    protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException, RepositoryException {

        DocumentHandle handle = getDocumentHandle(scInstance);

        if (handle.getRequest() == null) {
            try {
                PublishableDocument contextVariant = eval(scInstance, getContextVariantExpr());
                Date targetDate = null;

                if (getTargetDateExpr() != null) {
                    targetDate = eval(scInstance, getTargetDateExpr());
                }

                PublicationRequest req = null;

                if (targetDate == null) {
                    req = new PublicationRequest(getType(), contextVariant.getNode(), contextVariant, handle.getUser());
                } else {
                    req = new PublicationRequest(getType(), contextVariant.getNode(), contextVariant, handle.getUser(), targetDate);
                }

                req.getNode().getSession().save();
                handle.setRequest(req);
            } catch (RepositoryException e) {
                throw new ModelException("request publication failure", e);
            }
        } else {
            throw new ModelException("publication request already pending");
        }
     }

}
