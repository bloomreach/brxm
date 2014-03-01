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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom workflow task for archiving document.
 */
public class ArchiveDocumentTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ArchiveDocumentTask.class);

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dh = getDocumentHandle();
        DocumentVariant variant;
        try {
            variant = dh.getDocuments().get(HippoStdNodeType.DRAFT);
            if (variant != null) {
                deleteDocument(variant);
            }

            variant = dh.getDocuments().get(HippoStdNodeType.PUBLISHED);
            if (variant != null) {
                deleteDocument(variant);
            }
        } catch (RepositoryException e) {
            throw new WorkflowException(e.getMessage(), e);
        }

        try {
            variant = dh.getDocuments().get(HippoStdNodeType.UNPUBLISHED);
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowContext().getWorkflow("core", variant);
            defaultWorkflow.archive();
        } catch (MappingException e) {
            log.warn("Cannot archive document: no default workflow", e);
        }

        return null;
    }

    protected void deleteDocument(Document document) throws RepositoryException {
        Node node = document.getCheckedOutNode(getWorkflowContext().getInternalWorkflowSession());
        JcrUtils.ensureIsCheckedOut(node.getParent());
        node.remove();
    }

}
