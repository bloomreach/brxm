/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.jackrabbit.version;

import org.apache.jackrabbit.core.InternalXAResource;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.TransactionContext;
import org.apache.jackrabbit.core.TransactionException;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.XAItemStateManager;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.virtual.VirtualPropertyState;

import javax.jcr.version.Version;
import java.util.HashMap;
import java.util.Map;

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
import org.apache.jackrabbit.core.version.VersionManager;

/**
 * Implementation of a {@link VersionManager} that works in an XA environment.
 * Works as a filter between a version manager client and the global version
 * manager.
 */
public class XAVersionManager extends org.apache.jackrabbit.core.version.XAVersionManager
        implements EventStateCollectionFactory, VirtualItemStateProvider, InternalXAResource, AbstractVersionManager {

    /**
     * Attribute name for associated change log.
     */
    private static final String CHANGE_LOG_ATTRIBUTE_NAME = "XAVersionManager.ChangeLog";

    /**
     * Attribute name for items.
     */
    private static final String ITEMS_ATTRIBUTE_NAME = "VersionItems";

    /**
     * Repository version manager.
     */
    private final VersionManagerImpl vMgr;

    /**
     * The session that uses this version manager.
     */
    private SessionImpl session;

    /**
     * Items that have been modified and are part of the XA environment.
     */
    private Map xaItems;

    /**
     * flag that indicates if the version manager was locked during prepare
     */
    private boolean vmgrLocked = false;

    /**
     * Creates a new instance of this class.
     */
    public XAVersionManager(VersionManagerImpl vMgr, NodeTypeRegistry ntReg,
                            SessionImpl session, ItemStateCacheFactory cacheFactory)
            throws RepositoryException {
        super(vMgr, ntReg, session, cacheFactory);
        this.ntReg = ntReg;
        this.vMgr = vMgr;
        this.session = session;
        this.stateMgr = new XAItemStateManager(vMgr.getSharedStateMgr(),
                this, CHANGE_LOG_ATTRIBUTE_NAME, cacheFactory);

        NodeState state;
        try {
            state = (NodeState) stateMgr.getItemState(vMgr.getHistoryRootId());
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to retrieve history root", e);
        }
        this.historyRoot = new NodeStateEx(stateMgr, ntReg, state, NameConstants.JCR_VERSIONSTORAGE);
    }

    //------------------------------------------< EventStateCollectionFactory >

    /**
     * @inheritDoc
     */
    public EventStateCollection createEventStateCollection()
            throws RepositoryException {
        return vMgr.getHippoEscFactory().createEventStateCollection(session);
    }

    //-------------------------------------------------------< VersionManager >

    /**
     * {@inheritDoc}
     */
    public VirtualItemStateProvider getVirtualItemStateProvider() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public VersionHistory createVersionHistory(Session session, NodeState node)
            throws RepositoryException {

        if (isInXA()) {
            InternalVersionHistory history = createVersionHistory(node);
            xaItems.put(history.getId(), history);
            return (VersionHistory) ((SessionImpl) session).getNodeById(history.getId());
        }
        return vMgr.createVersionHistory(session, node);
    }

    /**
     * {@inheritDoc}
     */
    public Version checkin(NodeImpl node) throws RepositoryException {
        if (isInXA()) {
            String histUUID = node.getProperty(NameConstants.JCR_VERSIONHISTORY).getString();
            InternalVersion version = checkin((InternalVersionHistoryImpl)
                    getVersionHistory(NodeId.valueOf(histUUID)), node);
            return (Version) ((SessionImpl) node.getSession()).getNodeById(version.getId());
        }
        return vMgr.checkin(node);
    }

    /**
     * {@inheritDoc}
     */
    public void removeVersion(VersionHistory history, Name versionName)
            throws RepositoryException {
        if (isInXA()) {
            InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                    VersionManagerImpl.getInternalVersionHistory((VersionHistoryImpl) history);
            removeVersion(vh, versionName);
            return;
        }
        vMgr.removeVersion(history, versionName);
    }

    /**
     * {@inheritDoc}
     */
    public Version setVersionLabel(VersionHistory history, Name version,
                                   Name label, boolean move)
            throws RepositoryException {
        if (isInXA()) {
            InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                    VersionManagerImpl.getInternalVersionHistory((VersionHistoryImpl) history);
            InternalVersion v = setVersionLabel(vh, version, label, move);
            if (v == null) {
                return null;
            } else {
                return (Version) ((SessionImpl) history.getSession()).getNodeById(v.getId());
            }
        }
        return vMgr.setVersionLabel(history, version, label, move);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws Exception {
        stateMgr.dispose();
    }

    //---------------------------------------------< VirtualItemStateProvider >

    /**
     * {@inheritDoc}
     */
    public boolean isVirtualRoot(ItemId id) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getVirtualRootId() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public VirtualPropertyState createPropertyState(VirtualNodeState parent,
                                                    Name name, int type,
                                                    boolean multiValued)
            throws RepositoryException {

        throw new IllegalStateException("Read-only");
    }

    /**
     * {@inheritDoc}
     */
    public VirtualNodeState createNodeState(VirtualNodeState parent, Name name,
                                            NodeId id, Name nodeTypeName)
            throws RepositoryException {

        throw new IllegalStateException("Read-only");
    }

    /**
     * {@inheritDoc}
     */
    public boolean setNodeReferences(NodeReferences refs) {
        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            changeLog.modified(refs);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Return item states for changes only. Global version manager will return
     * other items.
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            return changeLog.get(id);
        }
        throw new NoSuchItemStateException("State not in change log: " + id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItemState(ItemId id) {
        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            return changeLog.has(id);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            return changeLog.get(id);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeReferencesId id) {
        ChangeLog changeLog = ((XAItemStateManager) stateMgr).getChangeLog();
        if (changeLog != null) {
            return changeLog.get(id) != null;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Not needed.
     */
    public void addListener(ItemStateListener listener) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Not needed.
     */
    public void removeListener(ItemStateListener listener) {
    }

    //-----------------------------------------------< AbstractVersionManager >

    /**
     * {@inheritDoc}
     */
     public InternalVersionItem getItem(NodeId id) throws RepositoryException {
        InternalVersionItem item = null;
        if (xaItems != null) {
            item = (InternalVersionItem) xaItems.get(id);
        }
        if (item == null) {
            item = vMgr.getItem(id);
        }
        return item;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItem(NodeId id) {
        if (xaItems != null && xaItems.containsKey(id)) {
            return true;
        }
        return vMgr.hasItem(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItemReferences(InternalVersionItem item)
            throws RepositoryException {
        return session.getNodeById(item.getId()).getReferences().hasNext();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Before modifying version history given, make a local copy of it.
     */
    public InternalVersion checkin(InternalVersionHistoryImpl history,
                                      NodeImpl node)
            throws RepositoryException {

        if (history.getVersionManager() != this) {
            history = makeLocalCopy(history);
            xaItems.put(history.getId(), history);
        }
        InternalVersion version = internalCheckin(history, node);
        NodeId frozenNodeId = version.getFrozenNodeId();
        InternalVersionItem frozenNode = createInternalVersionItem(frozenNodeId);
        if (frozenNode != null) {
            xaItems.put(frozenNodeId, frozenNode);
        }
        return version;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Before modifying version history given, make a local copy of it.
     */
    public void removeVersion(InternalVersionHistoryImpl history, Name name)
            throws VersionException, RepositoryException {

        if (history.getVersionManager() != this) {
            history = makeLocalCopy(history);
            xaItems.put(history.getId(), history);
            // also put 'successor' and 'predecessor' version items to xaItem sets
            InternalVersion v = history.getVersion(name);
            InternalVersion[] vs = v.getSuccessors();
            for (int i=0; i<vs.length; i++) {
                xaItems.put(vs[i].getId(), vs[i]);
            }
            vs = v.getPredecessors();
            for (int i=0; i<vs.length; i++) {
                xaItems.put(vs[i].getId(), vs[i]);
            }
        }
        internalRemoveVersion(history, name);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Before modifying version history given, make a local copy of it.
     */
    public InternalVersion setVersionLabel(InternalVersionHistoryImpl history,
                                              Name version, Name label,
                                              boolean move)
            throws RepositoryException {

        if (history.getVersionManager() != this) {
            history = makeLocalCopy(history);
            xaItems.put(history.getId(), history);
        }
        return internalSetVersionLabel(history, version, label, move);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Put the version object into our cache.
     */
    public void versionCreated(InternalVersion version) {
        xaItems.put(version.getId(), version);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Remove the version object from our cache.
     */
    public void versionDestroyed(InternalVersion version) {
        xaItems.remove(version.getId());
    }

    //-------------------------------------------------------------------< XA >

    /**
     * {@inheritDoc}
     */
    public void associate(TransactionContext tx) {
        ((XAItemStateManager) stateMgr).associate(tx);

        Map xaItems = null;
        if (tx != null) {
            xaItems = (Map) tx.getAttribute(ITEMS_ATTRIBUTE_NAME);
            if (xaItems == null) {
                xaItems = new HashMap();
                tx.setAttribute(ITEMS_ATTRIBUTE_NAME, xaItems);
            }
        }
        this.xaItems = xaItems;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Delegate the call to our XA item state manager.
     */
    public void beforeOperation(TransactionContext tx) {
        ((XAItemStateManager) stateMgr).beforeOperation(tx);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Delegate the call to our XA item state manager.
     */
    public void prepare(TransactionContext tx) throws TransactionException {
        if (vmgrLocked) {
            ((XAItemStateManager) stateMgr).prepare(tx);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Delegate the call to our XA item state manager. If successful, inform
     * global repository manager to update its caches.
     */
    public void commit(TransactionContext tx) throws TransactionException {
        if (vmgrLocked) {
            ((XAItemStateManager) stateMgr).commit(tx);
            Map xaItems = (Map) tx.getAttribute(ITEMS_ATTRIBUTE_NAME);
            vMgr.itemsUpdated(xaItems.values());
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Delegate the call to our XA item state manager.
     */
    public void rollback(TransactionContext tx) {
        if (vmgrLocked) {
            ((XAItemStateManager) stateMgr).rollback(tx);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Delegate the call to our XA item state manager.
     */
    public void afterOperation(TransactionContext tx) {
        ((XAItemStateManager) stateMgr).afterOperation(tx);
    }

    /**
     * Returns an {@link InternalXAResource} that acquires a write lock on the
     * version manager in {@link InternalXAResource#prepare(TransactionContext)}
     * if there are any version related items involved in this transaction.
     *
     * @return an internal XA resource.
     */
    public InternalXAResource getXAResourceBegin() {
        return new InternalXAResource() {
            public void associate(TransactionContext tx) {
            }

            public void beforeOperation(TransactionContext tx) {
            }

            public void prepare(TransactionContext tx) {
                Map vItems = (Map) tx.getAttribute(ITEMS_ATTRIBUTE_NAME);
                if (!vItems.isEmpty()) {
                    vMgr.acquireWriteLock();
                    vMgr.getSharedStateMgr().setNoLockHack(true);
                    vmgrLocked = true;
                }
            }

            public void commit(TransactionContext tx) {
            }

            public void rollback(TransactionContext tx) {
            }

            public void afterOperation(TransactionContext tx) {
            }
        };
    }

    /**
     * Returns an {@link InternalXAResource} that releases the write lock on the
     * version manager in {@link InternalXAResource#commit(TransactionContext)}
     * or {@link InternalXAResource#rollback(TransactionContext)}.
     *
     * @return an internal XA resource.
     */
    public InternalXAResource getXAResourceEnd() {
        return new InternalXAResource() {
            public void associate(TransactionContext tx) {
            }

            public void beforeOperation(TransactionContext tx) {
            }

            public void prepare(TransactionContext tx) {
            }

            public void commit(TransactionContext tx) {
                internalReleaseWriteLock();
            }

            public void rollback(TransactionContext tx) {
                internalReleaseWriteLock();
            }

            public void afterOperation(TransactionContext tx) {
            }

            private void internalReleaseWriteLock() {
                if (vmgrLocked) {
                    vMgr.getSharedStateMgr().setNoLockHack(false);
                    vMgr.releaseWriteLock();
                    vmgrLocked = false;
                }
            }
        };
    }

    //-------------------------------------------------------< implementation >

    /**
     * Return a flag indicating whether this version manager is currently
     * associated with an XA transaction.
     */
    private boolean isInXA() {
        return xaItems != null;
    }

    /**
     * Make a local copy of an internal version item. This will recreate the
     * (global) version item with state information from our own state
     * manager.
     */
    private InternalVersionHistoryImpl makeLocalCopy(InternalVersionHistoryImpl history)
            throws RepositoryException {
        acquireReadLock();
        try {
            NodeState state = (NodeState) stateMgr.getItemState(history.getId());
            NodeStateEx stateEx = new NodeStateEx(stateMgr, ntReg, state, null);
            return new InternalVersionHistoryImpl(this, stateEx);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to make local copy", e);
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Return a flag indicating whether an internal version item belongs to
     * a different XA environment.
     */
    boolean differentXAEnv(InternalVersionItemImpl item) {
        if (item.getVersionManager() == this) {
            if (xaItems == null || !xaItems.containsKey(item.getId())) {
                return true;
            }
        }
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Logger instance.
     */
    private static Logger log = LoggerFactory.getLogger(AbstractVersionManager.class);

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
    public InternalVersion internalCheckin(InternalVersionHistoryImpl history, NodeImpl node)
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
    public void internalRemoveVersion(InternalVersionHistoryImpl history, Name name)
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
    public InternalVersion internalSetVersionLabel(InternalVersionHistoryImpl history,
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
     * Invoked by the internal version item itself, when it's underlying
     * persistence state was discarded.
     *
     * @param item
     */
    public void itemDiscarded(InternalVersionItem item) {
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
