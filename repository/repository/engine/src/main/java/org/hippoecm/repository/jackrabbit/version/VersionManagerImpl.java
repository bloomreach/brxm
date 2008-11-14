/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit.ver;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.cluster.UpdateEventChannel;
import org.apache.jackrabbit.core.cluster.UpdateEventListener;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.observation.DelegatingObservationDispatcher;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.version.Version;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.InvalidItemStateException;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.VersionHistoryImpl;
import org.apache.jackrabbit.core.version.VersionImpl;
import org.apache.jackrabbit.core.version.VersionItemStateManager;
import org.apache.jackrabbit.core.version.VersionManager;

/**
 * This Class implements a VersionManager.
 */
public class VersionManagerImpl extends org.apache.jackrabbit.core.version.VersionManagerImpl implements ItemStateListener, UpdateEventListener, AbstractVersionManager {

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(VersionManager.class);

    /**
     * The path to the version storage: /jcr:system/jcr:versionStorage
     */
    private static final Path VERSION_STORAGE_PATH;

    static {
        try {
            PathBuilder builder = new PathBuilder();
            builder.addRoot();
            builder.addLast(NameConstants.JCR_SYSTEM);
            builder.addLast(NameConstants.JCR_VERSIONSTORAGE);
            VERSION_STORAGE_PATH = builder.getPath();
        } catch (MalformedPathException e) {
            // will not happen. path is always valid
            throw new InternalError("Cannot initialize path");
        }
    }

    /**
     * The persistence manager for the versions
     */
    private final PersistenceManager pMgr;

    /**
     * The file system for this version manager
     */
    private final FileSystem fs;

    /**
     * the version state manager for the version storage
     */
    private VersionItemStateManager sharedStateMgr;

    /**
     * the virtual item state provider that exposes the version storage
     */
    private final VersionItemStateProvider versProvider;

    /**
     * the dynamic event state collection factory
     */
    private final DynamicESCFactory escFactory;

    /**
     * Map of returned items. this is kept for invalidating
     */
    private final ReferenceMap versionItems = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);

    /**
     * Creates a new version manager
     *
     */
    public VersionManagerImpl(PersistenceManager pMgr, FileSystem fs,
                              NodeTypeRegistry ntReg,
                              DelegatingObservationDispatcher obsMgr, NodeId rootId,
                              NodeId rootParentId,
                              ItemStateCacheFactory cacheFactory,
                              ISMLocking ismLocking) throws RepositoryException {
        super(pMgr, fs, ntReg, obsMgr, rootId, rootParentId, cacheFactory, ismLocking);
        this.ntReg = ntReg;
        try {
            this.pMgr = pMgr;
            this.fs = fs;
            this.escFactory = new DynamicESCFactory(obsMgr);

            // need to store the version storage root directly into the persistence manager
            if (!pMgr.exists(rootId)) {
                NodeState root = pMgr.createNew(rootId);
                root.setParentId(rootParentId);
                root.setDefinitionId(ntReg.getEffectiveNodeType(NameConstants.REP_SYSTEM).getApplicableChildNodeDef(
                        NameConstants.JCR_VERSIONSTORAGE, NameConstants.REP_VERSIONSTORAGE, ntReg).getId());
                root.setNodeTypeName(NameConstants.REP_VERSIONSTORAGE);
                PropertyState pt = pMgr.createNew(new PropertyId(rootId, NameConstants.JCR_PRIMARYTYPE));
                pt.setDefinitionId(ntReg.getEffectiveNodeType(NameConstants.REP_SYSTEM).getApplicablePropertyDef(
                        NameConstants.JCR_PRIMARYTYPE, PropertyType.NAME, false).getId());
                pt.setMultiValued(false);
                pt.setType(PropertyType.NAME);
                pt.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_VERSIONSTORAGE)});
                root.addPropertyName(pt.getName());
                ChangeLog cl = new ChangeLog();
                cl.added(root);
                cl.added(pt);
                pMgr.store(cl);
            }
            sharedStateMgr = createItemStateManager(pMgr, rootId, ntReg, cacheFactory, ismLocking);

            stateMgr = new LocalItemStateManager(sharedStateMgr, escFactory, cacheFactory);
            stateMgr.addListener(this);

            NodeState nodeState = (NodeState) stateMgr.getItemState(rootId);
            historyRoot = new NodeStateEx(stateMgr, ntReg, nodeState, NameConstants.JCR_VERSIONSTORAGE);

            // create the virtual item state provider
            versProvider = new VersionItemStateProvider(
                    getHistoryRootId(), sharedStateMgr);

        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public VirtualItemStateProvider getVirtualItemStateProvider() {
        return versProvider;
    }
    
    /**
     * Return the persistence manager.
     * 
     * @return the persistence manager
     */
    public PersistenceManager getPersistenceManager() {
        return pMgr;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws Exception {
        pMgr.close();
        fs.close();
    }

    /**
     * Returns the event state collection factory.
     * @return the event state collection factory.
     */
    public org.apache.jackrabbit.core.version.VersionManagerImpl.DynamicESCFactory getEscFactory() {
        return null;
    }

    public DynamicESCFactory getHippoEscFactory() {
        return escFactory;
    }
    
    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public VersionHistory createVersionHistory(Session session, final NodeState node)
            throws RepositoryException {
        InternalVersionHistory history = (InternalVersionHistory)
                escFactory.doSourced((SessionImpl) session, new SourcedTarget(){
            public Object run() throws RepositoryException {
                return createVersionHistory(node);
            }
        });

        if (history == null) {
            throw new VersionException("History already exists for node " + node.getNodeId());
        }
        return (VersionHistory) ((SessionImpl) session).getNodeById(history.getId());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItem(NodeId id) {
        acquireReadLock();
        try {
            return stateMgr.hasItemState(id);
        } finally {
            releaseReadLock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionItem getItem(NodeId id)
            throws RepositoryException {

        if (id.equals(getHistoryRootId())) {
            return null;
        }
        acquireReadLock();
        try {
            synchronized (versionItems) {
                InternalVersionItem item = (InternalVersionItem) versionItems.get(id);
                if (item == null) {
                    item = createInternalVersionItem(id);
                    if (item != null) {
                        versionItems.put(id, item);
                    } else {
                        return null;
                    }
                }
                return item;
            }
        } finally {
            releaseReadLock();
        }
    }
    
    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public Version checkin(final NodeImpl node) throws RepositoryException {
        InternalVersion version = (InternalVersion)
                escFactory.doSourced((SessionImpl) node.getSession(), new SourcedTarget(){
            public Object run() throws RepositoryException {
                String histUUID = node.getProperty(NameConstants.JCR_VERSIONHISTORY).getString();
                return checkin((InternalVersionHistoryImpl)
                        getVersionHistory(NodeId.valueOf(histUUID)), node);
            }
        });

        return (VersionImpl)
                ((SessionImpl) node.getSession()).getNodeById(version.getId());
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public void removeVersion(VersionHistory history, final Name name)
            throws VersionException, RepositoryException {

        /*
        final VersionHistoryImpl historyImpl = (VersionHistoryImpl) history;
        if (!historyImpl.hasNode(name)) {
            throw new VersionException("Version with name " + name.toString()
                    + " does not exist in this VersionHistory");
        }

        escFactory.doSourced((SessionImpl) history.getSession(), new SourcedTarget(){
            public Object run() throws RepositoryException {
                InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                        historyImpl.getInternalVersionHistory();
                removeVersion(vh, name);
                return null;
            }
        });
        */
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public Version setVersionLabel(final VersionHistory history,
                                   final Name version, final Name label,
                                   final boolean move)
            throws RepositoryException {

        /*
        InternalVersion v = (InternalVersion)
                escFactory.doSourced((SessionImpl) history.getSession(), new SourcedTarget(){
            public Object run() throws RepositoryException {
                InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                        ((VersionHistoryImpl) history).getInternalVersionHistory();
                return setVersionLabel(vh, version, label, move);
            }
        });

        if (v == null) {
            return null;
        } else {
            return (Version)
                    ((SessionImpl) history.getSession()).getNodeByUUID(v.getId().getUUID());
        }
        */
        return null;
    }

    /**
     * Invoked by some external source to indicate that some items in the
     * versions tree were updated. Version histories are reloaded if possible.
     * Matching items are removed from the cache.
     *
     * @param items items updated
     */
    public void itemsUpdated(Collection items) {
        acquireReadLock();
        try {
            synchronized (versionItems) {
                Iterator iter = items.iterator();
                while (iter.hasNext()) {
                    InternalVersionItem item = (InternalVersionItem) iter.next();
                    InternalVersionItem cached = (InternalVersionItem) versionItems.remove(item.getId());
                    if (cached != null) {
                        if (cached instanceof InternalVersionHistoryImpl) {
                            InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl) cached;
                            try {
                                vh.reload();
                                versionItems.put(vh.getId(), vh);
                            } catch (RepositoryException e) {
                                log.warn("Unable to update version history: " + e.toString());
                            }
                        }
                    }
                }
            }
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Set an event channel to inform about updates.
     *
     * @param eventChannel event channel
     */
    public void setEventChannel(UpdateEventChannel eventChannel) {
        sharedStateMgr.setEventChannel(eventChannel);
        eventChannel.setListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public void itemDiscarded(InternalVersionItem item) {
        // evict removed item from cache
        acquireReadLock();
        try {
            versionItems.remove(item.getId());
        } finally {
            releaseReadLock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItemReferences(InternalVersionItem item)
            throws RepositoryException {
        try {
            NodeReferences refs = stateMgr.getNodeReferences(
                    new NodeReferencesId(item.getId()));
            return refs.hasReferences();
        } catch (NoSuchItemStateException e) {
            return false;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * returns the id of the version history root node
     *
     * @return the id of the version history root node
     */
    NodeId getHistoryRootId() {
        return historyRoot.getState().getNodeId();
    }

    /**
     * Return the shared item state manager.
     */
    protected SharedItemStateManager getSharedStateMgr() {
        return sharedStateMgr;
    }

    /**
     * Creates a <code>VersionItemStateManager</code> or derivative.
     *
     * @param pMgr          persistence manager
     * @param rootId        root node id
     * @param ntReg         node type registry
     * @param cacheFactory  cache factory
     * @param ismLocking    the ISM locking implementation
     * @return item state manager
     * @throws ItemStateException if an error occurs
     */
    public VersionItemStateManager createItemStateManager(PersistenceManager pMgr,
                                                             NodeId rootId,
                                                             NodeTypeRegistry ntReg,
                                                             ItemStateCacheFactory cacheFactory,
                                                             ISMLocking ismLocking)
            throws ItemStateException {
        return new VersionItemStateManager(pMgr, rootId, ntReg, cacheFactory, ismLocking);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Not used.
     */
    public void stateCreated(ItemState created) {}

    /**
     * {@inheritDoc}
     * <p/>
     * Not used.
     */
    public void stateModified(ItemState modified) {}

    /**
     * {@inheritDoc}
     * <p/>
     * Remove item from cache on removal.
     */
    public void stateDestroyed(ItemState destroyed) {
        // evict removed item from cache
        acquireReadLock();
        try {
            versionItems.remove(destroyed.getId());
        } finally {
            releaseReadLock();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Not used.
     */
    public void stateDiscarded(ItemState discarded) {}

    //--------------------------------------------------< UpdateEventListener >

    /**
     * {@inheritDoc}
     */
    public void externalUpdate(ChangeLog changes, List events) throws RepositoryException {
        EventStateCollection esc = getEscFactory().createEventStateCollection(null);
        esc.addAll(events);

        sharedStateMgr.externalUpdate(changes, esc);
    }

    //--------------------------------------------------------< inner classes >

    public static final class DynamicESCFactory implements EventStateCollectionFactory {

        /**
         * the observation manager
         */
        private DelegatingObservationDispatcher obsMgr;

        /**
         * the current event source
         */
        private SessionImpl source;


        /**
         * Creates a new event state collection factory
         * @param obsMgr
         */
        public DynamicESCFactory(DelegatingObservationDispatcher obsMgr) {
            this.obsMgr = obsMgr;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * This object uses one instance of a <code>LocalItemStateManager</code>
         * to update data on behalf of many sessions. In order to maintain the
         * association between update operation and session who actually invoked
         * the update, an internal event source is used.
         */
        public synchronized EventStateCollection createEventStateCollection()
                throws RepositoryException {
            if (source == null) {
                throw new RepositoryException("Unknown event source.");
            }
            return createEventStateCollection(source);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * This object uses one instance of a <code>LocalItemStateManager</code>
         * to update data on behalf of many sessions. In order to maintain the
         * association between update operation and session who actually invoked
         * the update, an internal event source is used.
         */
        public EventStateCollection createEventStateCollection(SessionImpl source) {
            return obsMgr.createEventStateCollection(source, VERSION_STORAGE_PATH);
        }

        /**
         * Executes the given runnable using the given event source.
         *
         * @param eventSource
         * @param runnable
         * @throws RepositoryException
         */
        public synchronized Object doSourced(SessionImpl eventSource, SourcedTarget runnable)
                throws RepositoryException {
            this.source = eventSource;
            try {
                return runnable.run();
            } finally {
                this.source = null;
            }
        }
    }

    private abstract class SourcedTarget {
        public abstract Object run() throws RepositoryException;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * State manager for the version storage.
     */
    protected LocalItemStateManager stateMgr;

    /**
     * Node type registry.
     */
    protected final NodeTypeRegistry ntReg;

    /**
     * Persistent root node of the version histories.
     */
    protected NodeStateEx historyRoot;

    /**
     * the lock on this version manager
     */
    private final ReadWriteLock rwLock =
            new ReentrantWriterPreferenceReadWriteLock() {
                /**
                 * Allow reader when there is no active writer, or current
                 * thread owns the write lock (reentrant).
                 */
                protected boolean allowReader() {
                    return activeWriter_ == null
                        || activeWriter_ == Thread.currentThread();
                }
            };

    //-------------------------------------------------------< VersionManager >

    /**
     * {@inheritDoc}
     */
    public InternalVersion getVersion(NodeId id) throws RepositoryException {
        // lock handling via getItem()
        InternalVersion v = (InternalVersion) getItem(id);
        if (v == null) {
            log.warn("Versioning item not found: " + id);
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionHistory getVersionHistory(NodeId id)
            throws RepositoryException {
        // lock handling via getItem()
        return (InternalVersionHistory) getItem(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasVersionHistory(NodeId id) {
        // lock handling via hasItem()
        return hasItem(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasVersion(NodeId id) {
        // lock handling via hasItem()
        return hasItem(id);
    }

    //-------------------------------------------------------< implementation >

    /**
     * aquires the write lock on this version manager.
     */
    public void acquireWriteLock() {
        while (true) {
            try {
                rwLock.writeLock().acquire();
                return;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * releases the write lock on this version manager.
     */
    public void releaseWriteLock() {
        rwLock.writeLock().release();
    }

    /**
     * aquires the read lock on this version manager.
     */
    public void acquireReadLock() {
        while (true) {
            try {
                rwLock.readLock().acquire();
                return;
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * releases the read lock on this version manager.
     */
    public void releaseReadLock() {
        rwLock.readLock().release();
    }

    /**
     * Helper for managing write operations.
     */
    private class WriteOperation {

        /**
         * Flag for successful completion of the write operation.
         */
        private boolean success = false;

        /**
         * Saves the pending operations in the {@link LocalItemStateManager}.
         *
         * @throws ItemStateException if the pending state is invalid
         * @throws RepositoryException if the pending state could not be persisted
         */
        public void save() throws ItemStateException, RepositoryException {
            stateMgr.update();
            success = true;
        }

        /**
         * Closes the write operation. The pending operations are cancelled
         * if they could not be properly saved. Finally the write lock is
         * released.
         */
        public void close() {
            try {
                if (!success) {
                    // update operation failed, cancel all modifications
                    stateMgr.cancel();
                }
            } finally {
                releaseWriteLock();
            }
        }
    }

    /**
     * Starts a write operation by acquiring the write lock and setting the
     * item state manager to the "edit" state. If something goes wrong, the
     * write lock is released and an exception is thrown.
     * <p>
     * The pattern for using this method and the returned helper instance is:
     * <pre>
     *     WriteOperation operation = startWriteOperation();
     *     try {
     *         ...
     *         operation.save(); // if everything is OK
     *         ...
     *     } catch (...) {
     *         ...
     *     } finally {
     *         operation.close();
     *     }
     * </pre>
     *
     * @return write operation helper
     * @throws RepositoryException if the write operation could not be started
     */
    private WriteOperation startWriteOperation() throws RepositoryException {
        boolean success = false;
        acquireWriteLock();
        try {
            stateMgr.edit();
            success = true;
            return new WriteOperation();
        } catch (IllegalStateException e) {
            throw new RepositoryException("Unable to start edit operation.", e);
        } finally {
            if (!success) {
                releaseWriteLock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public VersionHistory getVersionHistory(Session session, NodeState node)
            throws RepositoryException {
        acquireReadLock();
        try {
            NodeId vhId = getVersionHistoryId(node);
            if (vhId == null) {
                return null;
            }
            return (VersionHistory) ((SessionImpl) session).getNodeById(vhId);
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Creates a new Version History.
     *
     * @param node the node for which the version history is to be initialized
     * @return the newly created version history.
     * @throws javax.jcr.RepositoryException
     */
    public InternalVersionHistory createVersionHistory(NodeState node)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            // create deep path
            String uuid = node.getNodeId().getUUID().toString();
            NodeStateEx root = historyRoot;
            for (int i = 0; i < 3; i++) {
                Name name = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, uuid.substring(i * 2, i * 2 + 2));
                if (!root.hasNode(name)) {
                    root.addNode(name, NameConstants.REP_VERSIONSTORAGE, null, false);
                    root.store();
                }
                root = root.getNode(name, 1);
                if (root == null) {
                    throw new InvalidItemStateException();
                }
            }
            Name historyNodeName = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, uuid);
            if (root.hasNode(historyNodeName)) {
                // already exists
                return null;
            }

            // create new history node in the persistent state
            InternalVersionHistoryImpl hist = InternalVersionHistoryImpl.create(
                    this, root, new NodeId(UUID.randomUUID()), historyNodeName, node);

            // end update
            operation.save();

            log.debug("Created new version history " + hist.getId() + " for " + node + ".");
            return hist;

        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            operation.close();
        }
    }

    /**
     * Returns the id of the version history associated with the given node
     * or <code>null</code> if that node doesn't have a version history.
     *
     * @param node the node whose version history's id is to be returned.
     * @return the the id of the version history associated with the given node
     *         or <code>null</code> if that node doesn't have a version history.
     * @throws javax.jcr.RepositoryException if an error occurs
     */
    private NodeId getVersionHistoryId(NodeState node)
            throws RepositoryException {
        // build and traverse path
        String uuid = node.getNodeId().getUUID().toString();
        NodeStateEx n = historyRoot;
        for (int i = 0; i < 3; i++) {
            Name name = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, uuid.substring(i * 2, i * 2 + 2));
            if (!n.hasNode(name)) {
                return null;
            }
            n = n.getNode(name, 1);
        }
        Name historyNodeName = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, uuid);
        if (!n.hasNode(historyNodeName)) {
            return null;
        }
        return n.getNode(historyNodeName, 1).getNodeId();
    }

    /**
     * Checks in a node
     *
     * @param history the version history
     * @param node node to checkin
     * @return internal version
     * @throws javax.jcr.RepositoryException if an error occurs
     * @see javax.jcr.Node#checkin()
     */
    public InternalVersion checkin(InternalVersionHistoryImpl history, NodeImpl node)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            String versionName = calculateCheckinVersionName(history, node);
            InternalVersionImpl v = history.checkin(NameFactoryImpl.getInstance().create("", versionName), node);
            operation.save();
            return v;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            operation.close();
        }
    }

    /**
     * Calculates the name of the new version that will be created by a
     * checkin call. The name is determined as follows:
     * <ul>
     * <li> first the predecessor version with the shortes name is searched.
     * <li> if that predecessor version is the root version, the new version gets
     *      the name "{number of successors}+1" + ".0"
     * <li> if that predecessor version has no successor, the last digit of it's
     *      version number is incremented.
     * <li> if that predecessor version has successors but the incremented name
     *      does not exist, that name is used.
     * <li> otherwise a ".0" is added to the name until a non conflicting name
     *      is found.
     * <ul>
     *
     * Example Graph:
     * <xmp>
     * jcr:rootVersion
     *  |     |
     * 1.0   2.0
     *  |
     * 1.1
     *  |
     * 1.2 ---\  ------\
     *  |      \        \
     * 1.3   1.2.0   1.2.0.0
     *  |      |
     * 1.4   1.2.1 ----\
     *  |      |        \
     * 1.5   1.2.2   1.2.1.0
     *  |      |        |
     * 1.6     |     1.2.1.1
     *  |-----/
     * 1.7
     * </xmp>
     *
     * @param history the version history
     * @param node the node to checkin
     * @return the new version name
     * @throws RepositoryException if an error occurs.
     */
    public String calculateCheckinVersionName(InternalVersionHistoryImpl history,
                                                 NodeImpl node)
            throws RepositoryException {
        // 1. search a predecessor, suitable for generating the new name
        Value[] values = node.getProperty(NameConstants.JCR_PREDECESSORS).getValues();
        InternalVersion best = null;
        for (int i = 0; i < values.length; i++) {
            InternalVersion pred = history.getVersion(NodeId.valueOf(values[i].getString()));
            if (best == null
                    || pred.getName().getLocalName().length() < best.getName().getLocalName().length()) {
                best = pred;
            }
        }
        // 2. generate version name (assume no namespaces in version names)
        String versionName = best.getName().getLocalName();
        int pos = versionName.lastIndexOf('.');
        if (pos > 0) {
            String newVersionName = versionName.substring(0, pos + 1)
                + (Integer.parseInt(versionName.substring(pos + 1)) + 1);
            while (history.hasVersion(NameFactoryImpl.getInstance().create("", newVersionName))) {
                versionName += ".0";
                newVersionName = versionName;
            }
            return newVersionName;
        } else {
            // best is root version
            return String.valueOf(best.getSuccessors().length + 1) + ".0";
        }
    }

    /**
     * Removes the specified version from the history
     *
     * @param history the version history from where to remove the version.
     * @param name the name of the version to remove.
     * @throws javax.jcr.version.VersionException if the version <code>history</code> does
     *  not have a version with <code>name</code>.
     * @throws javax.jcr.RepositoryException if any other error occurs.
     */
    public void removeVersion(InternalVersionHistoryImpl history, Name name)
            throws VersionException, RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            history.removeVersion(name);
            operation.save();
        } catch (ItemStateException e) {
            log.error("Error while storing: " + e.toString());
        } finally {
            operation.close();
        }
    }

    /**
     * Set version label on the specified version.
     * @param history version history
     * @param version version name
     * @param label version label
     * @param move <code>true</code> to move from existing version;
     *             <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public InternalVersion setVersionLabel(InternalVersionHistoryImpl history,
                                              Name version, Name label,
                                              boolean move)
            throws RepositoryException {
        WriteOperation operation = startWriteOperation();
        try {
            InternalVersion v = history.setVersionLabel(version, label, move);
            operation.save();
            return v;
        } catch (ItemStateException e) {
            log.error("Error while storing: " + e.toString());
            return null;
        } finally {
            operation.close();
        }
    }

    /**
     * Invoked when a new internal item has been created.
     * @param version internal version item
     */
    public void versionCreated(InternalVersion version) {
    }

    /**
     * Invoked when a new internal item has been destroyed.
     * @param version internal version item
     */
    public void versionDestroyed(InternalVersion version) {
    }

    /**
     * Creates an {@link InternalVersionItem} based on the {@link NodeState}
     * identified by <code>id</code>.
     *
     * @param id    the node id of the version item.
     * @return the version item or <code>null</code> if there is no node state
     *         with the given <code>id</code>.
     * @throws RepositoryException if an error occurs while reading from the
     *                             version storage.
     */
    public InternalVersionItem createInternalVersionItem(NodeId id)
            throws RepositoryException {
        try {
            if (stateMgr.hasItemState(id)) {
                NodeState state = (NodeState) stateMgr.getItemState(id);
                NodeStateEx pNode = new NodeStateEx(stateMgr, ntReg, state, null);
                NodeId parentId = pNode.getParentId();
                InternalVersionItem parent = getItem(parentId);
                Name ntName = state.getNodeTypeName();
                if (ntName.equals(NameConstants.NT_FROZENNODE)) {
                    return new InternalFrozenNodeImpl(this, pNode, parent);
                } else if (ntName.equals(NameConstants.NT_VERSIONEDCHILD)) {
                    return new InternalFrozenVHImpl(this, pNode, parent);
                } else if (ntName.equals(NameConstants.NT_VERSION)) {
                    return ((InternalVersionHistory) parent).getVersion(id);
                } else if (ntName.equals(NameConstants.NT_VERSIONHISTORY)) {
                    return new InternalVersionHistoryImpl(this, pNode);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

}
