/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.replication;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.apache.jackrabbit.core.cluster.ChangeLogRecord;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.journal.RecordProducer;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @link {@link ReplicationUpdateEventListener} that can listen to internal or internal 
 * and external changes. The changes are passed to a {@link RecordProducer} which in turn 
 * writes the changes to a {@link Journal}.
 */
public class ReplicationJournalProducer implements ReplicationUpdateEventListener {

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(ReplicationJournalProducer.class);

    /**
     * Workspace name.
     */
    private final String workspace;

    /**
     * The Producer of the revision log.
     */
    private final RecordProducer producer;

    /**
     * Handle only internal repository changes or also external changes
     */
    private final boolean localChangesOnly;

    /**
     * Create a new instance for a specific workspace.
     * @param workspace the name of the {@link Workspace}.
     * @param producer the {@link RecordProducer}
     * @param localChangesOnly if only local changes should be written to the {@link Journal}
     */
    public ReplicationJournalProducer(String workspace, RecordProducer producer, boolean localChangesOnly) {
        this.workspace = workspace;
        this.producer = producer;
        this.localChangesOnly = localChangesOnly;
        log.info("Initialized replication journal producer for workspace='{}', localChangesOnly='{}'", workspace,
                localChangesOnly);
    }

    /**
     * {@inheritDoc}
     */
    public void externalUpdate(ChangeLog changes, List<EventState> events) throws RepositoryException {
        if (!localChangesOnly) {
            update(changes, events);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void internalUpdate(ChangeLog changes, List<EventState> events) throws RepositoryException {
        update(changes, events);
    }

    /**
     * Handle an update by passing the updates to the journal producer.
     *
     * @param changes external changes containing only node and property ids.
     * @param events events to deliver
     * @throws RepositoryException if the update cannot be processed
     */
    public void update(ChangeLog changes, List<EventState> events) throws RepositoryException {

        Record record = null;
        try {
            record = producer.append();
        } catch (JournalException e) {
            String msg = "Unable to append record: " + e.getMessage();
            log.error(msg, e);
        }
        if (record == null) {
            String msg = "No record created.";
            log.warn(msg);
            return;
        }

        boolean succeeded = false;

        try {
            ChangeLogRecord clr = new ChangeLogRecord(changes, events, record, workspace, System.currentTimeMillis(), null);
            clr.write();
            succeeded = true;
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while preparing log entry.";
            log.error(msg, e);
        } finally {
            if (!succeeded && record != null) {
                record.cancelUpdate();
                return;
            }
        }

        try {
            record.update();
            log.debug("Appended revision: {}", Long.valueOf(record.getRevision()));
        } catch (JournalException e) {
            String msg = "Unable to commit log entry.";
            log.error(msg, e);
        } catch (Throwable e) {
            String msg = "Unexpected error while committing log entry.";
            log.error(msg, e);
        }
    }

}
