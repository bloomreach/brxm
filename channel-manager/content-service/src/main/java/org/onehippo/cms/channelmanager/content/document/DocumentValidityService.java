/*
 * Copyright 2022 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.document;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;

public interface DocumentValidityService {

    /**
     * <p>
     * When a document type is changed, existing documents of said type might have an invalid content state, i.e. in
     * certain cases (compound fields) they are missing a node which prevents the VisualEditor from showing the fields
     * in the frontend. This method will check if a there are missing nodes, and will copy them from the document
     * prototype or the field prototype until the min-values amount is reached.
     * </p>
     * <p>
     * If there are changes made, the implementation of this method will persist the changes made by the
     * workflowSession.
     * </p>
     *
     * @param workflowSession The workflow session which has more write access on jcr nodes than a user session
     * @param documentType    The type of the document
     * @param variants        List of document variants. The first variant (normally the draft) in the list will be used
     *                        to find the missing prototypes.
     * @throws RepositoryException Throws a {@code RepositoryException} when the session save fails.
     */
    void handleDocumentTypeChanges(Session workflowSession, DocumentType documentType, final List<Node> variants)
            throws RepositoryException;
}
