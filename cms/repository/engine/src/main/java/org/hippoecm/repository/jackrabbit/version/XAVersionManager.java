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

import org.apache.jackrabbit.core.InternalXAResource;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.TransactionContext;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.NodeStateEx;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Value;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.uuid.UUID;

/**
 * Implementation of a {@link VersionManager} that works in an XA environment.
 * Works as a filter between a version manager client and the global version
 * manager.
 */
public class XAVersionManager extends org.apache.jackrabbit.core.version.XAVersionManager
        implements EventStateCollectionFactory, VirtualItemStateProvider, InternalXAResource, HippoVersionManager {

    private static final String CHANGE_LOG_ATTRIBUTE_NAME = "XAVersionManager.ChangeLog";

    private static final String ITEMS_ATTRIBUTE_NAME = "VersionItems";


    public XAVersionManager(VersionManagerImpl vMgr, NodeTypeRegistry ntReg,
                            SessionImpl session, ItemStateCacheFactory cacheFactory)
            throws RepositoryException {
        super(vMgr, ntReg, session, cacheFactory);
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

            return hist;

        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            operation.close();
        }
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
    public Version checkin(NodeImpl node) throws RepositoryException {
        if (isInXA()) {
            String histUUID = node.getProperty(NameConstants.JCR_VERSIONHISTORY).getString();
            InternalVersion version = checkin((InternalVersionHistoryImpl)
                    getVersionHistory(NodeId.valueOf(histUUID)), node);
            return (Version) ((SessionImpl) node.getSession()).getNodeById(version.getId());
        } else
            return super.checkin(node);
    }

    private boolean isInXA = false;
        private boolean isInXA() {
        return isInXA;
    }
        @Override
    public void associate(TransactionContext tx) {
        isInXA = (tx != null);
    }
    @Override
     public InternalVersionItem getItem(NodeId id) throws RepositoryException {
         return super.getItem(id);
    }

    /*
    protected boolean hasItem(NodeId id) {
        if (xaItems != null && xaItems.containsKey(id)) {
            return true;
        }
        return vMgr.hasItem(id);
    }
*/
     @Override
     public boolean hasItemReferences(InternalVersionItem item)
            throws RepositoryException {
        return super.hasItemReferences(item); // return session.getNodeById(item.getId()).getReferences().hasNext();
    }

  /*  public void versionCreated(InternalVersion version) {
        xaItems.put(version.getId(), version);
    }

    public void versionDestroyed(InternalVersion version) {
        xaItems.remove(version.getId());
    }*/
     @Override
     public void versionCreated(InternalVersion version) {
         super.versionCreated(version);
     }
     @Override
public void versionDestroyed(InternalVersion version) {
    super.versionDestroyed(version);
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
}
