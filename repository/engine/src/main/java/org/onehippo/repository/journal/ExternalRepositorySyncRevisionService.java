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

import javax.jcr.RepositoryException;

/**
 * ExternalRepositorySyncRevisionService provides access to additional and custom DatabaseJournal revision entries
 * storing revision ids for external repository synchronization processes like replication.
 * <p>
 * The primary reason for storing these revision ids is to guard and protect against cleanup of Journal entries which
 * have <em>not yet</em> been synchronized (like replicated) to an external repository.
 * </p>
 * <p>
 * Typically (and possibly even automatically) cleanup of no longer needed Journal entries is done by querying the
 * LOCAL_REVISIONS database table and deleting all entries below the lowest recorded (processed) revision id.
 * </p>
 * <p>
 * By recording an additional entry in the LOCAL_REVISIONS table, using a known {@link #EXTERNAL_REPOSITORY_SYNC_ID_PREFIX}
 * id name prefix to distinguish these from 'normal' cluster node ids, external repository synchronization processes
 * like replication, can mark which revision has been synchronized so far, thereby protecting against not-yet-processed
 * Journal entries from been cleaned up.
 * </p>
 * <p>
 * This service provides access to a dedicated {@link #getSyncRevision(String) ExternalRepositorySynRevision} instance
 * which can be used to read and set such an external repository synchronization revision.
 * </p>
 */
public interface ExternalRepositorySyncRevisionService {

    /**
     * Prefix used for namespacing external repository sync ids in the LOCAL_REVISIONS database table.
     */
    String EXTERNAL_REPOSITORY_SYNC_ID_PREFIX = "_HIPPO_EXTERNAL_REPO_SYNC_";

    /**
     * Get access to the ExternalRepositorySyncRevision instance for a specific id.
     * <p>
     * Note that when the repository instance has <em>NOT</em> been configured as a clustered node, or <em>NOT</em>
     * using a Jackrabbit DatabaseJournal no value (null) will be returned!
     * </p>
     * <p>
     * Note that the provide id parameter will be <em>prefixed</em> by {@link #EXTERNAL_REPOSITORY_SYNC_ID_PREFIX} to
     * ensure proper isolation and protection against overwriting/overlaying a real repository cluster node id.
     * </p>
     * <p>
     * <em>Choosing a proper and uniquely defined custom id is critical as the entry value in the database will get
     * overwritten. Accidentally overlaying an (not proper named) cluster node id or using the same id for different
     * purposes will lead to unexpected/corrupted behavior!</em>
     * </p>
     * @param id a custom (and unique within the repository <em>cluster</em> revision entry id
     * @return the Revision instance for the provided id, or null if no DatabaseJournal has been configured.
     * @throws IllegalArgumentException when the provided id value is null or (trimmed) is empty or the
     *         {@link ExternalRepositorySyncRevision#getQualifiedId()} length would be longer than 255 characters
     * @throws RepositoryException
     */
    ExternalRepositorySyncRevision getSyncRevision(String id) throws IllegalArgumentException, RepositoryException;
}
