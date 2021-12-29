/*
 * Copyright 2021 Bloomreach (http://www.bloomreach.com)
 */

package org.onehippo.cms.channelmanager.content.document;

import javax.jcr.Node;
import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;

public interface DocumentValidityService {

    /**
     * <p>
     * When a document type is changed, existing documents of said type might have an invalid content state, i.e.
     * in certain cases (compound fields) they are missing a child node which prevents the VisualEditor from
     * showing the fields in the frontend. This method will check if a document is missing such nodes, and will
     * copy them from the document prototype or the field prototype until the min-values amount is reached.
     * </p>
     *
     * <p>It will try to add the missing nodes to both the "draft" and the "unpublished" version of the document.</p>
     *
     * @param workflowSession   The workflow session which has more write access on jcr nodes than a user session
     * @param branchId          The project branch of the document
     * @param documentHandle    The handle of the document backed by the workflow session
     * @param documentType      The type of the document
     */
    void handleDocumentTypeChanges(Session workflowSession, String branchId, Node documentHandle, DocumentType documentType);
}
