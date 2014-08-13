/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webresources;


import java.util.List;

/**
 * A web resource contains binary data that can be revisioned. There is always a current working version of the content.
 * There can be zero or more revisions of the content. Each revision is identified by an ID that is unique within
 * the revision history of this web resource.
 */
public interface WebResource {

    /**
     * @return the absolute path to this web resource, starting at web resources root.
     * The path always starts with a slash, and the path elements are also separated by slashes.
     */
    String getPath();

    /**
     * @return the name of this web resource, i.e. the last element of the path.
     */
    String getName();

    /**
     * @return the current content of this web resource. Use {@link #createRevision()} to create a revision
     * of the content.
     */
    Content getContent();

    /**
     * Updates the content of this web resource.
     * @param binary the binary data to store.
     * @return whether the content actually changed (e.g. the new binary data differs from the existing
     * binary data)
     * @throws java.lang.IllegalArgumentException if the given binary is <code>null</code>.
     */
    boolean setContent(Binary binary);

    /**
     * Creates a new revision of this web resource's content. Afterwards the current content will have been added to the
     * revision history, and the latest revision and the current content will be identical.
     * @return the ID of the created revision.
     * @throws WebResourceException when an error occurs.
     */
    String createRevision();

    /**
     * @return all revision IDs of this web resource's content. If no revisions exist yet, an empty list will be returned.
     * Revision IDs are unique within the revision history of a web resource.
     */
    List<String> getRevisionIds();

    /**
     * @return the ID of the most recently created revision, or <code>null</code> if no revision has been created yet.
     */
    String getLatestRevisionId();

    /**
     * @param revisionId the ID of the revision to return. When the revision ID is <code>null</code>, this call is
     *                   identical to {@link #getContent()}.
     * @return a revision of the content of this web resource, or the current working version of the content when the
     * revision ID is null.
     */
    Content getContent(String revisionId) throws RevisionNotFoundException;

    /**
     * Removes this web resource.
     * @throws WebResourceException in case the delete did not occur, for example because the resource is not there any more,
     * insufficient authorization, locking, etc
     */
    void delete() throws WebResourceException;

}
