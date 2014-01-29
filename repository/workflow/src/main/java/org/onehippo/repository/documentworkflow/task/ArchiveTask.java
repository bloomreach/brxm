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
public class ArchiveTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ArchiveTask.class);

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dm = getDocumentHandle();
        DocumentVariant variant;
        try {
            variant = dm.getDocumentVariantByState(HippoStdNodeType.DRAFT);
            if (variant != null) {
                deleteDocument(variant);
            }

            variant = dm.getDocumentVariantByState(HippoStdNodeType.PUBLISHED);
            if (variant != null) {
                deleteDocument(variant);
            }
        } catch (RepositoryException e) {
            throw new WorkflowException(e.getMessage(), e);
        }

        try {
            variant = dm.getDocumentVariantByState(HippoStdNodeType.UNPUBLISHED);
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) dm.getWorkflowContext().getWorkflow("core", variant);
            defaultWorkflow.archive();
        } catch (MappingException ex) {
            log.warn("invalid default workflow, falling back in behaviour", ex);
        }

        return null;
    }

    protected void deleteDocument(Document document) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(document.getNode());
        JcrUtils.ensureIsCheckedOut(document.getNode().getParent());
        document.getNode().remove();
    }

}
