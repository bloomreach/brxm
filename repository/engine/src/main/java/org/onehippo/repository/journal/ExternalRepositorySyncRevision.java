/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository.journal;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

/**
 * A ExternalRepositorySyncRevision instance provides access to a custom revision entry in the LOCAL_REVISIONS table.
 */
public interface ExternalRepositorySyncRevision {

    /**
     * The unique external repository sync revision id
     * @return The unique external repository sync revision id
     */
    String getId();

    /**
     * The fully qualified unique external repository sync revision id as stored in the LOCAL_REVISIONS table,
     * which is the {@link #getId()} prefixed with {@link ExternalRepositorySyncRevisionService#EXTERNAL_REPOSITORY_SYNC_ID_PREFIX}
     * @return The fully qualified unique external repository sync revision id as stored in the LOCAL_REVISIONS table
     */
    String getQualifiedId();

    /**
     * Indicates if for this instance an entry in the LOCAL_REVISIONS table exists.
     * <p>
     * Only when this returns true {@link #get()} may be invoked.
     * </p>
     * @return true if for this instance an entry in the LOCAL_REVISIONS table exists
     */
    boolean exists();

    /**
     * Returns the current revision value for this instance as stored in the database.
     * @return the current revision value for this instance as stored in the database.
     * @throws IllegalStateException when there is no entry in the LOCAL_REVISIONS table yet.
     *         First use {@link #exists()} before calling this method.
     */
    long get() throws IllegalStateException;

    /**
     * Store the provided revision value for this instance in the database.
     * @param revision the revision value to store
     * @throws RepositoryException when the value could not be stored
     */
    void set(long revision) throws RepositoryException;
}
