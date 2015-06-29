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

package org.apache.jackrabbit.core.journal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.util.db.DbUtility;
import org.onehippo.repository.journal.ExternalRepositorySyncRevision;
import org.onehippo.repository.journal.ExternalRepositorySyncRevisionService;

/**
 * ExternalRepositorySyncRevisionService implementation which is provided in the same Jackrabbit package as the
 * Jackrabbit DatabaseJournal class, to allow access to its protected members and methods via the
 * {@link DatabaseJournalAccessor} inner class.
 */
public class ExternalRepositorySyncRevisionServiceImpl implements ExternalRepositorySyncRevisionService {

    private static class DatabaseJournalAccessor {
        private final DatabaseJournal dj;

        public DatabaseJournalAccessor(final DatabaseJournal dj) {
            this.dj = dj;
        }

        public long get(final String qualifiedId) throws RepositoryException {
            ResultSet rs = null;
            try {
                // Check whether there is an entry in the database.
                rs = dj.conHelper.exec(dj.getLocalRevisionStmtSQL, new Object[]{qualifiedId}, false, 0);
                boolean exists = rs.next();
                if (exists) {
                    return rs.getLong(1);
                }
                throw new ItemNotFoundException(qualifiedId);
            }
            catch (SQLException e) {
                throw new RepositoryException("Failed to access revision for "+qualifiedId, e);
            }
            finally {
                DbUtility.close(rs);
            }
        }

        public void init(final String qualifiedId, final long revision) throws RepositoryException {
            try {
                dj.conHelper.exec(dj.insertLocalRevisionStmtSQL, revision, qualifiedId);
            }
            catch (SQLException e) {
                throw new RepositoryException("Failed to initialize revision for "+qualifiedId, e);
            }
        }

        public void set(final String qualifiedId, final long revision) throws RepositoryException {
            try {
                dj.conHelper.exec(dj.updateLocalRevisionStmtSQL, revision, qualifiedId);
            }
            catch (SQLException e) {
                throw new RepositoryException("Failed to update revision for "+qualifiedId, e);
            }
        }
    }

    private static class ExternalRepositorySyncRevisionImpl implements ExternalRepositorySyncRevision {

        private final DatabaseJournalAccessor djAccessor;
        private final String id;
        private final String qualifiedId;
        private long  revision;
        private boolean exists;

        public ExternalRepositorySyncRevisionImpl(final DatabaseJournalAccessor djAccessor, final String id) throws RepositoryException {
            if (id == null || id.trim().length() == 0) {
                throw new IllegalArgumentException("id must be not null and not empty");
            }
            this.djAccessor = djAccessor;
            this.id = id.trim();
            this.qualifiedId = EXTERNAL_REPOSITORY_SYNC_ID_PREFIX + id;
            if (this.qualifiedId.length() > 255) {
                throw new IllegalArgumentException("Invalid qualified id length > 255: "+this.qualifiedId);
            }
            try {
                revision = djAccessor.get(qualifiedId);
                exists = true;
            }
            catch (ItemNotFoundException e) {
                // not yet defined
            }
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getQualifiedId() {
            return qualifiedId;
        }

        @Override
        public synchronized boolean exists() {
            return exists;
        }

        @Override
        public synchronized long get() throws IllegalStateException {
            if (!exists) {
                throw new IllegalStateException("Revision entry not yet created");
            }
            return revision;
        }

        @Override
        public synchronized void set(final long revision) throws RepositoryException {
            if (!exists) {
                djAccessor.init(qualifiedId, revision);
                exists = true;
            }
            else {
                djAccessor.set(qualifiedId, revision);
            }
            this.revision = revision;
        }
    }

    private Map<String, ExternalRepositorySyncRevision> revisionsMap = new HashMap<>();

    private final DatabaseJournalAccessor djAccessor;

    public ExternalRepositorySyncRevisionServiceImpl(final Journal dj) {
        this.djAccessor = dj instanceof DatabaseJournal ? new DatabaseJournalAccessor((DatabaseJournal)dj) : null;
    }

    public synchronized ExternalRepositorySyncRevision getSyncRevision(final String id) throws IllegalArgumentException, RepositoryException {
        ExternalRepositorySyncRevision syncRevision = revisionsMap.get(id);
        if (syncRevision == null && djAccessor != null) {
            syncRevision = new ExternalRepositorySyncRevisionImpl(djAccessor, id);
            revisionsMap.put(id, syncRevision);
        }
        return syncRevision;
    }
}
