/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.repository.translation.impl;

import java.util.Map;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;

/**
 * Determines if a user is allowed to add translations.
 */
final class AddTranslationHint {

    /** Map that contains the siblings and the userSubject. */
    private final Map<String, DocumentVariant> documents;
    /** Document variant that is used to determine if a user is allowed to add translations. */
    private final Node documentVariant;

    public static Boolean canAddTranslation(final Node userSubject) throws WorkflowException, RepositoryException {
        return !new AddTranslationHint(userSubject).hideTranslation();
    }

    private AddTranslationHint(final Node userSubject) throws WorkflowException, RepositoryException {
        this.documentVariant = Objects.requireNonNull(userSubject);
        final Node handle = userSubject.getParent();
        final DocumentHandle documentHandle = new DocumentHandle(handle);
        documentHandle.initialize();
        documents = documentHandle.getDocuments();
    }

    private Boolean hideTranslation() throws RepositoryException {
        return isDraft() || isTransferable() || ( hasUnpublished() && isPublished() );
    }

    private boolean isPublished() throws RepositoryException {
        return isState(HippoStdNodeType.PUBLISHED);
    }

    private boolean hasUnpublished() {
        return hasState(HippoStdNodeType.UNPUBLISHED);
    }

    private Boolean isTransferable() throws RepositoryException {
        return hasDraft() && Boolean.TRUE.equals(documents.get(HippoStdNodeType.DRAFT).isTransferable());
    }

    private Boolean hasDraft() {
        return hasState(HippoStdNodeType.DRAFT);
    }

    private boolean hasState(final String state) {
        return documents.containsKey(state);
    }

    private Boolean isDraft() throws RepositoryException {
        return isState(HippoStdNodeType.DRAFT);
    }

    private Boolean isState(final String state)
            throws RepositoryException {
        return hasState(state)
                && documents.get(state).getNode().getIdentifier().equals(documentVariant.getIdentifier());
    }

}
