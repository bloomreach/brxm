/*
 *  Copyright 2010 Hippo.
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.core.journal.FileJournal;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.journal.RecordConsumer;
import org.apache.jackrabbit.core.journal.RecordIterator;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;

/**
 * The replication journal keeps track of all the changes made in the repository
 * on file system. This is based on the {@link FileJournal} as used for the jackrabbit 
 * clustering.<br/>
 * The difference with the cluster journal is that cluster journals do not process changes
 * created be the same journal (producer). 
 */
public class ReplicationJournal extends FileJournal {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(ReplicationJournal.class);

    /**
     * Default (relative to repHome) directory for journal files.
     */
    private static final String DEFAULT_DIRECTORY = "replication";

    /**
     * Journal lock, allowing multiple readers (synchronizing their contents)
     * but only one writer (appending a new entry).
     */
    private final ReadWriteLock rwLock = new ReentrantWriterPreferenceReadWriteLock();

    /**
     * Map of registered consumers.
     */
    protected Map<String, RecordConsumer> consumers = new HashMap<String, RecordConsumer>();

    /**
     * Whether this journal is for local repository changes only 
     * or also contains updates from external (cluster nodes)
     */
    private boolean localChangesOnly = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(String id, NamespaceResolver resolver) throws JournalException {
        if (getDirectory() == null) {
            setDirectory(getRepositoryHome() + File.separator + DEFAULT_DIRECTORY);
        }
        log.info("Setting replication journal directory to: {}", getDirectory());
        super.init(id, resolver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(RecordConsumer consumer) throws JournalException {
        synchronized (consumers) {
            String consumerId = consumer.getId();
            log.debug("Registering consumer: {}", consumerId);
            if (consumers.containsKey(consumerId)) {
                String msg = "Record consumer with identifier '" + consumerId + "' already registered.";
                throw new JournalException(msg);
            }
            consumers.put(consumerId, consumer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unregister(RecordConsumer consumer) {
        synchronized (consumers) {
            String consumerId = consumer.getId();
            return consumers.remove(consumerId) != null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecordConsumer getConsumer(String identifier) {
        synchronized (consumers) {
            return (RecordConsumer) consumers.get(identifier);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sync(boolean startup) throws JournalException {
        // consumers  synchronize themselves
    }

    /**
     * Synchronize the consumer with the journal.
     * @param consumer
     * @throws JournalException
     */
    public void sync(RecordConsumer consumer) throws JournalException {
        try {
            getLock().readLock().acquire();
        } catch (InterruptedException e) {
            String msg = "Unable to acquire read lock.";
            throw new JournalException(msg, e);
        }
        try {
            doSync(consumer);
        } finally {
            getLock().readLock().release();
        }
    }

    /**
     * Do the actual sync. The read lock on the journal should already be acquired.
     * @param consumer
     * @throws JournalException
     * @see {@link #sync(RecordConsumer)}
     */
    protected void doSync(RecordConsumer consumer) throws JournalException {
        long stopRevision = Long.MIN_VALUE;
        log.debug("Syncing consumer {}", consumer.getId() + " [" + consumer.getRevision() + "]");
        RecordIterator recIter = getRecords(consumer.getRevision());
        while (recIter.hasNext()) {
            Record record = recIter.nextRecord();
            consumer.consume(record);
            stopRevision = record.getRevision();
            consumer.setRevision(stopRevision);
        }
        if (stopRevision > 0) {
            log.debug("Synchronized consumer {} to revision: {}", consumer.getId(), stopRevision);
        }
    }

    /**
     * {@inheritDoc}
     * Just get the write lock. The replicator nodes sync themselves.
     */
    @Override
    public void lockAndSync() throws JournalException {
        try {
            getLock().writeLock().acquire();
        } catch (InterruptedException e) {
            String msg = "Unable to acquire write lock.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlock(boolean successful) {
        doUnlock(successful);
        getLock().writeLock().release();
    }

    /**
     * Return the lock associated with the journal.
     * @return the lock
     */
    protected ReadWriteLock getLock() {
        return rwLock;
    }

    /**
     * Check if this journal should contain only local repository changes.
     * @return true is only local changes should be in the journal
     */
    public boolean getLocalChangesOnly() {
        return localChangesOnly;
    }

    /**
     * Set if this journal should contain only local changes.
     * @param localChangesOnly
     */
    public void setLocalChangesOnly(boolean localChangesOnly) {
        this.localChangesOnly = localChangesOnly;
    }

}
