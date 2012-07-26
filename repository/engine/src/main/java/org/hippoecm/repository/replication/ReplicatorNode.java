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
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.core.cluster.ChangeLogRecord;
import org.apache.jackrabbit.core.cluster.ClusterRecord;
import org.apache.jackrabbit.core.cluster.ClusterRecordDeserializer;
import org.apache.jackrabbit.core.cluster.ClusterRecordProcessor;
import org.apache.jackrabbit.core.cluster.LockRecord;
import org.apache.jackrabbit.core.cluster.NamespaceRecord;
import org.apache.jackrabbit.core.cluster.NodeTypeRecord;
import org.apache.jackrabbit.core.cluster.WorkspaceRecord;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.journal.FileRevision;
import org.apache.jackrabbit.core.journal.InstanceRevision;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.journal.RecordConsumer;
import org.hippoecm.repository.replication.config.FilterConfig;
import org.hippoecm.repository.replication.config.ReplicatorNodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * Each {@link ReplicatorNode} is running in it's own thread for replicating changes to a 
 * single remote repository.  The {@link ReplicatorNode} takes care of consuming the records from 
 * the {@link Journal} and keeping track of the last seen revision id and the life cycle management.
 * <p>The actual replication itself is handled by the associated 
 * {@link Replicator}.</p>
 */
public class ReplicatorNode implements Runnable, ClusterRecordProcessor, RecordConsumer {
    /** @exclude */

    /**
     * Status constant.
     */
    private static final int NONE = 0;

    /**
     * Status constant.
     */
    private static final int STARTED = 1;

    /**
     * Status constant.
     */
    private static final int STOPPED = 2;

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(ReplicatorNode.class);

    /**
     * Replicator node id.
     */
    private String id;

    /**
     * Journal used.
     */
    private ReplicationJournal journal;

    /**
     * Synchronization thread.
     */
    private Thread syncThread;

    /**
     * Mutex used when syncing.
     */
    private final Mutex syncLock = new Mutex();

    /**
     * Latch used to communicate a stop request to the synchronization thread.
     */
    private final Latch stopLatch = new Latch();

    /**
     * Status flag, one of {@link #NONE}, {@link #STARTED} or {@link #STOPPED}.
     */
    private volatile int status;

    /**
     * Instance revision manager.
     */
    private InstanceRevision instanceRevision;

    /**
     * Record deserializer.
     */
    private ClusterRecordDeserializer deserializer = new ClusterRecordDeserializer();

    /**
     * The {@link Replicator}.
     */
    private Replicator replicator;

    
    /**
     * The configuration
     */
    private final ReplicatorNodeConfig config;

    /**
     * Keep count of the amount of (unsuccessful) tries.
     */
    private int tries;
    
    /**
     * 
     * @param config
     * @throws ConfigurationException
     */
    public ReplicatorNode(ReplicatorNodeConfig config) throws ConfigurationException {
        this.config = config;
    }

    /**
     * Initialize this replicator node.
     *
     * @throws ConfigurationException if an error occurs
     */
    public void init(ReplicatorContext replicatorContext) throws ConfigurationException {
        setId(config.getId());

        List<Filter> filters = new ArrayList<Filter>();
        for (FilterConfig fc: config.getFilterConfigs()) {
            Filter filter = (Filter) fc.newInstance(Filter.class);
            filters.add(filter);
        }
        
        // start replicator.
        replicator = (Replicator) config.getReplicatorConfig().newInstance(Replicator.class);
        replicator.init(replicatorContext, filters);
        
        try {
            journal = replicatorContext.getJournal();
            instanceRevision = getInstanceRevision(replicatorContext.getReplicationHomeDir());
            journal.register(this);
        } catch (JournalException e) {
            throw new ConfigurationException("Unable to register the replicator node with the journal", e);
        }
    }

    /**
     * Return the journal created by this replicator node.
     *
     * @return journal
     */
    public Journal getJournal() {
        return journal;
    }

    /**
     * {@inheritDoc}
     */
    public InstanceRevision getInstanceRevision(String path) throws JournalException {
        return new FileRevision(new File(path + File.separator + getId() + "." + "revision"));
    }

    /**
     * Starts this replicator node.
     */
    public synchronized void start() {
        if (status == NONE) {
            Thread t = new Thread(this, "ReplicatorNode-" + id);
            t.setDaemon(true);
            t.start();
            syncThread = t;
            status = STARTED;
            log.info("Replicator {} started at revisions {}.", getId(), getRevision());
        }
    }

    /**
     * Run loop that will sync this node after some delay.
     */
    public void run() {
        for (;;) {
            try {
                if (stopLatch.attempt(getSyncDelay())) {
                    break;
                }
            } catch (InterruptedException e) {
                String msg = "Interrupted while waiting for stop latch.";
                log.warn(msg);
            }
            try {
                sync();
            } catch (JournalException e) {
                String msg = "Periodic sync of journal failed: " + e.getMessage();
                log.error(msg, e);
            } catch (Exception e) {
                String msg = "Unexpected error while syncing of journal: " + e.getMessage();
                log.error(msg, e);
            } catch (Error e) {
                String msg = "Unexpected error while syncing of journal: " + e.getMessage();
                log.error(msg, e);
                throw e;
            }
        }
    }

    /**
     * Synchronize contents from journal.
     *
     * @throws JournalException if an error occurs
     */
    public void sync() throws JournalException {
        try {
            syncLock.acquire();
        } catch (InterruptedException e) {
            String msg = "Interrupted while waiting for mutex.";
            throw new JournalException(msg);
        }

        try {
            journal.sync(this);
        } catch (JournalException e) {
            throw new JournalException(e.getMessage(), e.getCause());
        } finally {
            syncLock.release();
        }
    }

    public void waitWithoutLock(long timeout) throws JournalException {
        syncLock.release();
        journal.getLock().readLock().release();
        
        try {
            Thread.sleep(getRetryDelay());
        } catch (InterruptedException ex) {
            log.debug("Waking up for retry.");
        }

        try {
            journal.getLock().readLock().acquire();
        } catch (InterruptedException e) {
            String msg = "Unable to acquire read lock.";
            throw new JournalException(msg, e);
        }
        
        try {
            syncLock.acquire();
        } catch (InterruptedException e) {
            String msg = "Interrupted while waiting for mutex.";
            throw new JournalException(msg);
        }
    }
    
    
    
    /**
     * Stops this replicator node.
     */
    public synchronized void stop() {
        if (status != STOPPED) {
            status = STOPPED;

            stopLatch.release();

            // Give synchronization thread some time to finish properly before
            // closing down the journal (see JCR-1553)
            if (syncThread != null) {
                try {
                    syncThread.join(getStopDelay());
                } catch (InterruptedException e) {
                    String msg = "Interrupted while joining synchronization thread.";
                    log.warn(msg);
                }
            }
            if (instanceRevision != null) {
                instanceRevision.close();
            }
            log.info("Replicator " + getId() + " stopped.");
        }
    }

    protected String getIdString() {
        return getId() + " [" + getRevision() + "]";
    }

    //--------------------------------------------------- ClusterRecordProcessor

    /**
     * {@inheritDoc}
     */
    public void process(ChangeLogRecord record) {
        log.debug("{} processing workspace {} revision: {}", new Object[] { getIdString(), record.getWorkspace(),
                record.getRevision() });
        try {
            replicator.replicate(record);
        } catch (RecoverableReplicationException e) {
            // we need to try this again, but api doesn't allow checked exceptions, re-throw as runtime.
            throw new RetryReplicationException(e);
        } catch (FatalReplicationException e) {
            log.error("Unable to replicate revision: " + record.getRevision(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(LockRecord record) {
        // Not implemented
    }

    /**
     * {@inheritDoc}
     */
    public void process(NamespaceRecord record) {
        // Not implemented
    }

    /**
     * {@inheritDoc}
     */
    public void process(NodeTypeRecord record) {
        // Not implemented
    }

    public void process(WorkspaceRecord record) {
        // Not implemented
    }

    //-------------------------------------------------------< RecordConsumer >

    /**
     * {@inheritDoc}
     */
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public long getRevision() {
        try {
            return instanceRevision.get();
        } catch (JournalException e) {
            String msg = getIdString() + ": Unable to return current revision.";
            log.error(msg, e);
            return Long.MAX_VALUE;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The method also retries to replicate the change as defined with the 
     * retryDelay and numberOfRetries parameters.
     */
    public void consume(Record record) {
        if (status == STOPPED) {
            return;
        }
        
        log.debug("{} consuming revision: {}", getIdString(), record.getRevision());
        try {
            Exception error = null;
            ClusterRecord clusterRecored = deserializer.deserialize(record);
            while (tries <= getMaxRetries() && (status != STOPPED)) {
                error = null;
                try {
                    clusterRecored.process(this);
                    tries = 0;
                    return;
                } catch (RetryReplicationException e) {
                    tries++;
                    error = e;
                    if (tries <= getMaxRetries()) {
                        log.info("Recoverable exception (" + e.getMessage() + ") encountered with revision: "
                                + record.getRevision() + ". Trying again [" + tries + "/" + getMaxRetries() + "] in "
                                + getRetryDelay() + " ms.");
                        waitWithoutLock(getRetryDelay());
                    }
                }
            }
            String msg = getIdString() + ": Unable to replicate revision '" + record.getRevision() + "'.";
            if (error == null) {
                log.error(msg);
            } else {
                log.error(msg, error);
            }
        } catch (JournalException e) {
            String msg = getIdString() + ": Unable to read revision '" + record.getRevision() + "'.";
            log.error(msg, e);
        } finally {
            tries = 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRevision(long revision) {
        if (status != STOPPED) {
            try {
                instanceRevision.set(revision);
            } catch (JournalException e) {
                String msg = getIdString() + ": Unable to set current revision to '" + revision + "'.";
                log.error(msg, e);
            }
        }
    }

    //----------------------------------------------- Bean setters and getters

    public long getStopDelay() {
        return config.getStopDelay();
    }

    public long getSyncDelay() {
        return config.getSyncDelay();
    }

    public long getRetryDelay() {
        return config.getRetryDelay();
    }
    
    public int getMaxRetries() {
        return config.getMaxRetries();
    }

    public void setId(String id) throws ConfigurationException {
        if (id == null) {
            throw new ConfigurationException (
                    "Replicator node id not set. Pleasa configure the replicator node id in the replication config file.");
        }
        this.id = id;
    }

}
