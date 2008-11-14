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
package org.hippoecm.repository.jackrabbit.version;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.cluster.UpdateEventListener;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.DelegatingObservationDispatcher;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.NodeStateEx;
import org.apache.jackrabbit.core.version.VersionItemStateManager;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.VersionImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.uuid.UUID;

public class VersionManagerImpl extends org.apache.jackrabbit.core.version.VersionManagerImpl implements ItemStateListener, UpdateEventListener, HippoVersionManager {

    private static Logger log = LoggerFactory.getLogger(VersionManager.class);
    private final HippoDynamicESCFactory hippoEscFactory;
    
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

    public VersionManagerImpl(PersistenceManager pMgr, FileSystem fs,
            NodeTypeRegistry ntReg,
            DelegatingObservationDispatcher obsMgr, NodeId rootId,
            NodeId rootParentId,
            ItemStateCacheFactory cacheFactory,
            ISMLocking ismLocking) throws RepositoryException {
        super(pMgr, fs, ntReg, obsMgr, rootId, rootParentId, cacheFactory, ismLocking);
             this.hippoEscFactory = new HippoDynamicESCFactory(obsMgr);
   }

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

    private abstract class SourcedTarget {
        public abstract Object run() throws RepositoryException;
    }
    
        protected String calculateCheckinVersionName(InternalVersionHistoryImpl history,
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

    protected InternalVersion checkin(InternalVersionHistoryImpl history, NodeImpl node)
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

    @Override
    public Version checkin(final NodeImpl node) throws RepositoryException {
        InternalVersion version = (InternalVersion)
                hippoEscFactory.doSourced((SessionImpl) node.getSession(), new SourcedTarget(){
            public Object run() throws RepositoryException {
                String histUUID = node.getProperty(NameConstants.JCR_VERSIONHISTORY).getString();
                return checkin((InternalVersionHistoryImpl)
                        getVersionHistory(NodeId.valueOf(histUUID)), node);
            }
        });

        return (VersionImpl)                 ((SessionImpl) node.getSession()).getNodeById(version.getId());
    }


    class WriteOperation {

        private boolean success = false;

        public void save() throws ItemStateException, RepositoryException {
            stateMgr.update();
            success = true;
        }

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
     public static final class HippoDynamicESCFactory implements EventStateCollectionFactory {

    private DelegatingObservationDispatcher obsMgr;

        private SessionImpl source;

        public HippoDynamicESCFactory(DelegatingObservationDispatcher obsMgr) {
            this.obsMgr = obsMgr;
        }

        public synchronized EventStateCollection createEventStateCollection()
                throws RepositoryException {
            if (source == null) {
                throw new RepositoryException("Unknown event source.");
            }
            return createEventStateCollection(source);
        } 
        public EventStateCollection createEventStateCollection(SessionImpl source) {
            return obsMgr.createEventStateCollection(source, VERSION_STORAGE_PATH);
        }

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

    @Override
    public void itemDiscarded(InternalVersionItem item) {
        super.itemDiscarded(item);
    }

    public NodeId getHistoryRootId() {
        return historyRoot.getState().getNodeId();
    }

    @Override
    public SharedItemStateManager getSharedStateMgr() {
        return super.getSharedStateMgr();
    }

    @Override
    protected VersionItemStateManager createItemStateManager(PersistenceManager pMgr,
            NodeId rootId,
            NodeTypeRegistry ntReg,
            ItemStateCacheFactory cacheFactory,
            ISMLocking ismLocking)
            throws ItemStateException {
        return new VersionItemStateManager(pMgr, rootId, ntReg, cacheFactory, ismLocking);
    }

    @Override
    public void acquireReadLock() {
        super.acquireReadLock();
    }

    @Override
    public void releaseReadLock() {
        super.releaseReadLock();
    }

    @Override
    public void acquireWriteLock() {
        super.acquireReadLock();
    }

    @Override
    public void releaseWriteLock() {
        super.releaseReadLock();
    }

    @Override
    public void versionCreated(InternalVersion version) {
        super.versionCreated(version);
    }

    @Override
    public void versionDestroyed(InternalVersion version) {
        super.versionDestroyed(version);
    }

    @Override
    public boolean hasItemReferences(InternalVersionItem item)
            throws RepositoryException {
        return super.hasItemReferences(item);
    }

    @Override
    public InternalVersionItem getItem(NodeId id)
            throws RepositoryException {
        return super.getItem(id);
    }
    
    @Override
    protected InternalVersionItem createInternalVersionItem(NodeId id)
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
