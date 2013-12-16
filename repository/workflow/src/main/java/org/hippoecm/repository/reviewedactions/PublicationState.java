/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.reviewedactions;


import javax.jcr.RepositoryException;

public class PublicationState {

    private final PublishableDocument published;
    private final PublishableDocument unpublished;
    private final PublishableDocument draft;
    private final PublicationRequest request;

    public PublicationState(final PublishableDocument published, final PublishableDocument unpublished, final PublishableDocument draft, final PublicationRequest request) {
        this.published = published;
        this.unpublished = unpublished;
        this.draft = draft;
        this.request = request;
    }


    public boolean isEditing() throws RepositoryException {
        return draft != null && draft.getOwner() != null;
    }

    public boolean isLive() throws RepositoryException {
        return published != null && published.isAvailable("live");
    }

    public boolean isDirty() throws RepositoryException {
        return unpublished != null && (published == null || !isLive() || published.getLastModificationDate().getTime() == 0 || !published.getLastModificationDate().equals(unpublished.getLastModificationDate()));
    }

    public boolean isRequestPending() {
        return request != null;
    }

    public boolean isDraftInUse(String userIdentity) throws RepositoryException {
        return isEditing() && !draft.getOwner().equals(userIdentity);
    }

}
