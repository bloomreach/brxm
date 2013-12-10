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
package org.onehippo.repository.documentworkflow.task;

import java.rmi.RemoteException;
import java.util.Date;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.PublicationRequest;
import org.onehippo.repository.documentworkflow.PublishableDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom workflow task for publication request, depublication request, scheduled publication, scheduled depublication and deletion request.
 */
public class RequestWorkflowTask extends AbstractDocumentWorkflowTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(RequestWorkflowTask.class);

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
    public void doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dm = getDataModel();

        if (dm.getRequest() == null) {
            PublishableDocument contextVariant = eval(getContextVariantExpr());
            Date targetDate = null;

            if (getTargetDateExpr() != null) {
                targetDate = eval(getTargetDateExpr());
            }

            PublicationRequest req = null;

            if (targetDate == null) {
                req = new PublicationRequest(getType(), contextVariant.getNode(), contextVariant, dm.getUser());
            } else {
                req = new PublicationRequest(getType(), contextVariant.getNode(), contextVariant, dm.getUser(), targetDate);
            }

            req.getNode().getSession().save();
            dm.setRequest(req);
        } else {
            throw new WorkflowException("publication request already pending");
        }
     }

}
