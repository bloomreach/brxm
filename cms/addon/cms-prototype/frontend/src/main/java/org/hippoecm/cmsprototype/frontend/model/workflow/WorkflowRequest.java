/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.model.workflow;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.cmsprototype.frontend.model.content.DocumentVariant;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A workflow request object, associated with a {@link DocumentVariant}.
 *
 * A WorkflowRequest's JCR node is of node type "hippo:request".
 */
public class WorkflowRequest extends NodeModelWrapper {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(WorkflowRequest.class);

    public WorkflowRequest(JcrNodeModel nodeModel) throws ModelWrapException {
        super(nodeModel);
        try {
            if (!nodeModel.getNode().isNodeType(HippoNodeType.NT_REQUEST)) {
                throw new ModelWrapException("Node is not a request object.");
            }
        } catch (RepositoryException e) {
            throw new ModelWrapException(e);
        }
    }

    /**
     * @return The document variant this request object belongs to
     */
    public DocumentVariant getDocumentVariant() {
        javax.jcr.Session session = (javax.jcr.Session)(((UserSession)Session.get()).getJcrSession());
        String docUUID;
        try {
            if (nodeModel.getNode().hasProperty("document")) {
                docUUID = nodeModel.getNode().getProperty("document").getString();
                return new DocumentVariant(new JcrNodeModel(session.getNodeByUUID(docUUID)));
            }
            else {
                return null;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            return null;
        } catch (ModelWrapException e) {
            log.error(e.getMessage());
            return null;
        }
        
    }
    
}
