/*
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.ToStringBuilder;

public class DocumentHandle {

    private Map<String, Serializable> hints = new HashMap<String, Serializable>();
    private PublicationRequest request;
    private PublishableDocument draft;
    private PublishableDocument unpublished;
    private PublishableDocument published;
    private String user;
    private String workflowState;

    public DocumentHandle(String user, String workflowState) {
        this.user = user;
        this.workflowState = workflowState;
    }

    public String getUser() {
        return user;
    }

    public String getWorkflowState() {
        return workflowState;
    }

    public PublicationRequest getRequest() {
        return request;
    }

    public void setRequest(final PublicationRequest request) {
        this.request = request;
    }

    public void putDocumentVariant(PublishableDocument variant) throws RepositoryException {
        String state = variant.getState();

        if (PublishableDocument.DRAFT.equals(state)) {
            draft = variant;
        } else if (PublishableDocument.UNPUBLISHED.equals(state)) {
            unpublished = variant;
        } else if (PublishableDocument.PUBLISHED.equals(state)) {
            published = variant;
        }
    }

    public PublishableDocument getDocumentVariantByState(String state) {
        if (PublishableDocument.DRAFT.equals(state)) {
            return draft;
        } else if (PublishableDocument.UNPUBLISHED.equals(state)) {
            return unpublished;
        } else if (PublishableDocument.PUBLISHED.equals(state)) {
            return published;
        }

        return null;
    }

    public PublishableDocument getDraft() {
        return draft;
    }

    public void setDraft(final PublishableDocument draft) {
        this.draft = draft;
    }

    public PublishableDocument getUnpublished() {
        return unpublished;
    }

    public void setUnpublished(final PublishableDocument unpublished) {
        this.unpublished = unpublished;
    }

    public PublishableDocument getPublished() {
        return published;
    }

    public void setPublished(final PublishableDocument published) {
        this.published = published;
    }

    public Map<String, Serializable> getHints() {
            return hints;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
