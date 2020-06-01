/*
 * Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.onehippo.repository.journal.ChangeLog.Record;

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

    int NODE_MODIFIED = 0x80;

    /**
     * Get access to the ExternalRepositorySyncRevision instance for a specific key.
     * <p>
     * Note that when the repository instance has <em>NOT</em> been configured as a clustered node, or <em>NOT</em>
     * using a Jackrabbit DatabaseJournal no value (null) will be returned!
     * </p>
     * <p>
     * Note that the provide key parameter will be <em>prefixed</em> by {@link #EXTERNAL_REPOSITORY_SYNC_ID_PREFIX} to
     * ensure proper isolation and protection against overwriting/overlaying a real repository cluster node key.
     * </p>
     * <p>
     * <em>Choosing a proper and uniquely defined custom key is critical as the entry value in the database will get
     * overwritten. Accidentally overlaying an (not proper named) cluster node key or using the same key for different
     * purposes will lead to unexpected/corrupted behavior!</em>
     * </p>
     * @param key a custom (and unique within the repository <em>cluster</em> revision entry key
     * @return the Revision instance for the provided key, or null if no DatabaseJournal has been configured.
     * @throws IllegalArgumentException when the provided key value is null or (trimmed) is empty or the
     *         {@link ExternalRepositorySyncRevision#getQualifiedId()} length would be longer than 255 characters
     * @throws RepositoryException
     */
    ExternalRepositorySyncRevision getSyncRevision(String key) throws IllegalArgumentException, RepositoryException;


    /**
     * <p>
     *     Experimental, do not use in production since it needs hardening, see the comments in the implementation
     * </p>
     * <p>
     *     Note on the {@code session} the implementation will invoke
     *     {@link Session#refresh(boolean) session.refresh(true)} to force a cluster sync
     * </p>
     * <p>
     *     Note the ChangeLog startRevision can be larger than fromRevision if fromRevision does not exist any more
     * </p>
     * @param session the {@link Session} to get the change logs for
     * @param fromRevision the revision from which the {@link ChangeLog}s should be created
     * @param softLimit the 'soft' limit number of the total returned {@link Record}s in all
     *                  the returned change logs combined. It is a 'soft' limit because we never truncate a {@link ChangeLog}
     *                  to avoid a single bundle {@link Event#PERSIST} to be returned partially, hence the total number
     *                  of all records combined in all change logs can exceed the {@code softLimit}
     * @param squashEvents When {@code true} it means that *any* event for the same node gets squashed
     *                     into a single {@link Record}. The event type will then be
     *                     {@link #NODE_MODIFIED} which is not an existing {@link Event#getType()} but can be seen as
     *                     NODE_MODIFIED which can mean any property event of the node they belong too or any node change.
     *                     Note that this flag is a potentially important memory reduction trick since it squashes a
     *                     lot of the events into a single {@link Record}. The {@link Record#getPath()} will be the
     *                     path of the Node (the node of the property) and the {@link Record#getRevision()} will be
     *                     the revision of the last {@link Event} for the {@link javax.jcr.Node} (including its properties)
     * @param scopes The scopes to return change logs from (including the scope itself,
     *               for example a scope is /content/documents. If all events are needed, use empty scope (not /)
     * @param ignorePropertyNames The list of property names for which changes can be ignored.
     * @return The List of change logs, where change logs are separated by an {@link  Event#PERSIST}. At least one
     * {@link ChangeLog} will be present in the returned {@link List}. The <strong>LAST</strong> {@link ChangeLog} in the
     * return {@link List} can be a {@link ChangeLog} which does not have any recorded {@link Record}s : We still
     * return that {@link ChangeLog} since it contains valuable information with respect to the start and end revision
     */
    List<ChangeLog> getChangeLogs(Session session, long fromRevision,
                                  long softLimit, List<String> scopes, List<String> ignorePropertyNames,
                                  boolean squashEvents);

}
