/*
 * Copyright 2013-2023 Bloomreach
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
import java.util.Map;

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

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_BRANCHES_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;

/**
 * <p>Custom workflow task for archiving or deleting a document</p>
 *
 * <p>Archives a document (moves document to the attic) if
 * an unpublished variant is present. </p>
 * <p>Deletes a document (completely delete the handle) if
 * only a draft variant is present. That can happen is a document
 * has been saved as draft and subsequently is deleted.</p>
 */
public class ArchiveDocumentTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ArchiveDocumentTask.class);

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dh = getDocumentHandle();

        final Node handle = dh.getHandle();
        if (handle.isNodeType(NT_HIPPO_VERSION_INFO)) {
            // to be sure, not only remove the NT_HIPPO_VERSION_INFO but first explicitly remove the properties
            if (handle.hasProperty(HIPPO_BRANCHES_PROPERTY)) {
                handle.getProperty(HIPPO_BRANCHES_PROPERTY).remove();
            }
            if (handle.hasProperty(HIPPO_VERSION_HISTORY_PROPERTY)) {
                handle.getProperty(HIPPO_VERSION_HISTORY_PROPERTY).remove();
            }
            handle.removeMixin(NT_HIPPO_VERSION_INFO);
        }

        try {
            final Map<String, DocumentVariant> documents = dh.getDocuments();
            final DocumentVariant draft = documents.get(HippoStdNodeType.DRAFT);
            final DocumentVariant unpublished = documents.get(HippoStdNodeType.UNPUBLISHED);
            final DocumentVariant published = documents.get(HippoStdNodeType.PUBLISHED);
            if (published != null) {
                deleteDocument(published);
            }
            if (unpublished != null) {
                if (draft != null) {
                    deleteDocument(draft);
                }
                getDefaultWorkflow(unpublished).archive();
            } else if (draft != null) {
                getDefaultWorkflow(draft).delete();
            }
        } catch (MappingException mappingException) {
            log.warn("Cannot archive document: no default workflow", mappingException);
        } catch (RepositoryException e) {
            throw new WorkflowException(e.getMessage(), e);
        }

        return null;
    }

    private DefaultWorkflow getDefaultWorkflow(final DocumentVariant variant) throws WorkflowException, RepositoryException {
        return (DefaultWorkflow) getWorkflowContext().getWorkflow("core", variant);
    }

    protected void deleteDocument(Document document) throws RepositoryException {
        Node node = document.getCheckedOutNode(getWorkflowContext().getInternalWorkflowSession());
        JcrUtils.ensureIsCheckedOut(node.getParent());
        node.remove();
    }

}
