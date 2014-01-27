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
public class RequestTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(RequestTask.class);

    private String type;
    private PublishableDocument contextVariant;
    private Date targetDate;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PublishableDocument getContextVariant() {
        return contextVariant;
    }

    public void setContextVariant(PublishableDocument contextVariant) {
        this.contextVariant = contextVariant;
    }

    public Date getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dm = getDocumentHandle();

        if (dm.getRequest() == null) {
            PublicationRequest req;

            if (targetDate == null) {
                req = new PublicationRequest(getType(), contextVariant.getNode(), contextVariant, dm.getUser());
            } else {
                req = new PublicationRequest(getType(), contextVariant.getNode(), contextVariant, dm.getUser(), targetDate);
            }

            req.getNode().getSession().save();
//            dm.setRequest(req);
        } else {
            throw new WorkflowException("publication request already pending");
        }

        return null;
    }

}
