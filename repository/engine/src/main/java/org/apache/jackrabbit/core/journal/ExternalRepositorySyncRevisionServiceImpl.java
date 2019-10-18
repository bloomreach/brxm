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

package org.apache.jackrabbit.core.journal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.hippoecm.repository.api.RevisionEvent;
import org.hippoecm.repository.api.RevisionEventJournal;
import org.onehippo.repository.journal.ChangeLog;
import org.onehippo.repository.journal.ChangeLogImpl;
import org.onehippo.repository.journal.ExternalRepositorySyncRevision;
import org.onehippo.repository.journal.ExternalRepositorySyncRevisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;

/**
 * ExternalRepositorySyncRevisionService implementation which is provided in the same Jackrabbit package as the
 * Jackrabbit DatabaseJournal class, to allow access to its protected members and methods via the
 * {@link DatabaseJournalAccessor} inner class.
 */
public class ExternalRepositorySyncRevisionServiceImpl implements ExternalRepositorySyncRevisionService {

    private static Logger log = LoggerFactory.getLogger(ExternalRepositorySyncRevisionServiceImpl.class);

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

    public synchronized ExternalRepositorySyncRevision getSyncRevision(final String key) throws IllegalArgumentException, RepositoryException {
        ExternalRepositorySyncRevision syncRevision = revisionsMap.get(key);
        if (syncRevision == null && djAccessor != null) {
            syncRevision = new ExternalRepositorySyncRevisionImpl(djAccessor, key);
            revisionsMap.put(key, syncRevision);
        }
        return syncRevision;
    }

    @Override
    public List<ChangeLog> getChangeLogs(final Session session, final long fromRevision, final long softLimit,
                                         final List<String> scopes, final List<String> ignorePropertyNames, final boolean squashEvents) {

        synchronized (session) {
            try {
                log.debug("Reading the journal");
                triggerClusterSync(session);

                // TODO what happens if for the session there is also an observation listener? If we 'skip to revision'
                // TODO this means that events are removed AFAICS....does this have impact? Or should we say that the
                // TODO session of the argument is not allowed to be used in a listener? Or do we have to impersonate
                // TODO the session first to another session?

                // TODO See org.apache.jackrabbit.core.observation.ObservationManagerImpl.getEventJournal(int, java.lang.String, boolean,
                // TODO java.lang.String[], java.lang.String[]) : It seems the session has to be admin otherwise
                // TODO getEventJournal is not supported it seems
                RevisionEventJournal eventJournal = (RevisionEventJournal)session.getWorkspace().getObservationManager().getEventJournal();

                eventJournal.skipToRevision(fromRevision);

                final ArrayList<ChangeLog> changeLogs = new ArrayList<>();

                ChangeLogImpl changeLog = new ChangeLogImpl();


                int recordCount = 0;
                long lastEventRevision = fromRevision;

                while (eventJournal.hasNext()) {

                    RevisionEvent event = eventJournal.nextEvent();

                    lastEventRevision = event.getRevision();

                    if (!changeLogs.contains(changeLog)) {
                        changeLog.setStartRevision(lastEventRevision);
                        changeLogs.add(changeLog);
                    }

                    changeLog.setEndRevision(lastEventRevision);

                    if (event.getType() == Event.PERSIST) {
                        if (!changeLog.getRecords().isEmpty()) {
                            changeLog.setEndRevision(lastEventRevision);
                            // change log will only be added if there is at least one more event in next while loop
                            // and the limit is not yet reached
                            changeLog = new ChangeLogImpl();
                            changeLog.setStartRevision(lastEventRevision + 1);

                        }
                        if (recordCount < softLimit ) {
                            continue;
                        } else {
                            break;
                        }
                    }

                    final String path = event.getPath();
                    if (path == null) {
                        log.warn("Skipping unexpected event with path null: ", event);
                        continue;
                    }

                    if (isPropertyEvent(event)) {
                        final String propertyName = StringUtils.substringAfterLast(path, "/");
                        if (ignorePropertyNames.contains(propertyName)) {
                            log.debug("Skipping property event '{}'", event.getPath());
                            continue;
                        }
                    }

                    if (scopes.stream().anyMatch(scope -> path.startsWith(scope + "/") || path.equals(scope))) {
                        if (changeLog.recordChange(event, squashEvents)) {
                            recordCount++;
                        }
                    }

                }

                log.debug("Read {} changes up to {}", recordCount, lastEventRevision);
                return changeLogs;

            } catch (RepositoryException e) {
                log.error("Repository Exception while getting ChangesLogs", e);
                return Collections.emptyList();
            }

        }

    }

    private void triggerClusterSync(final Session session) throws RepositoryException {
        session.refresh(true);
    }

    private boolean isPropertyEvent(final RevisionEvent event) {
        return event.getType() == PROPERTY_REMOVED || event.getType() == PROPERTY_CHANGED || event.getType() == PROPERTY_ADDED;
    }
}
