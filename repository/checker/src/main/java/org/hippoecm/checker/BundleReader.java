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
package org.hippoecm.checker;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.BundleBinding;
import org.apache.jackrabbit.core.persistence.util.ErrorHandling;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle.ChildNodeEntry;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle.PropertyEntry;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.apache.jackrabbit.core.value.InternalValue;

class BundleReader extends DatabaseDelegate<NodeDescription> implements Visitable<NodeDescription>, BLOBStore, DataStore {;
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    boolean onlyReferenceable;
    Repair repair;

    BundleReader(PersistenceManager persistMgr, boolean onlyReferenceable, Repair repair) {
        super(persistMgr);
        this.onlyReferenceable = onlyReferenceable;
        this.repair = repair;
    }

    /**
     *
     */
    public void accept(Visitor<NodeDescription> visitor) {
        int maxCount = access.getBundleBatchSize();

        ResultSet rs = null;
        NodeId lastNodeId = null;

        boolean hadResults = true;

        while(hadResults) {
            try {
                hadResults = false;
                rs = getBundles(lastNodeId, maxCount);
                while (rs.next()) {
                    hadResults = true;
                    // do check
                    NodeId lastChecked = checkEntry(visitor, rs);
                    if (lastChecked != null) {
                        lastNodeId = lastChecked;
                    }
                }
            } catch (SQLException e) {
                log.error("FATAL ERROR RETRIEVING NODE SET", e);
                break;
            } finally {
                DbUtility.close(rs);
            }
        }
        log.info("No more nodes found: done.");
    }


    protected NodeId checkEntry(Visitor<NodeDescription> visitor, ResultSet rs) {
        try {
            Map.Entry<NodeId, InputStream> bundle = readEntry(rs);
            final NodeId nodeId = bundle.getKey();
            try {
                InputStream in = bundle.getValue();
                if (in != null) {
                    try {
                        readBundle(nodeId, in, visitor, onlyReferenceable);
                    } catch (IOException ex) {
                        Checker.log.error("Unable to load bundle content with id "+nodeId+": "+ex.getClass().getName()+": "+ex.getMessage(), ex);
                        repair.removeNode(Repair.RepairStatus.PENDING, create(nodeId));
                    }
                } else {
                    Checker.log.error("Unable to load bundle with id "+nodeId+" removing it");
                    repair.removeNode(Repair.RepairStatus.PENDING, create(nodeId));
                }
                return nodeId;
            } catch (Throwable ex) {
                Checker.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                repair.removeNode(Repair.RepairStatus.PENDING, create(nodeId));
            }
        } catch (Throwable ex) {
            Checker.log.error("FATAL ERROR RETRIEVING NODE ID: "+ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
        return null;
    }


    /**
     * Get ResultSet for bundles from store.
     * @param bigger last NodeId
     * @param maxCount
     * @return the ResultSet
     * @throws SQLException
     */
    protected ResultSet getBundles(NodeId bigger, int maxCount) throws SQLException {
        if (bigger == null) {
            return access.getConnectionHelper().exec(access.getBundleSelectAllSQL(), null, false, maxCount);
        } else {
            return access.getConnectionHelper().exec(access.getBundleSelectAllFromSQL(), getKey(bigger), false, maxCount);
        }
    }


    /**
     * Constructs a parameter list for a PreparedStatement
     * for the given node identifier.
     *
     * @param id the node id
     * @return a list of Objects
     */
    protected Object[] getKey(NodeId id) {
        if (access.getStorageModelBinaryKeys()) {
            return new Object[] { id.getRawBytes() };
        } else {
            return new Object[] { id.getMostSignificantBits(), id.getLeastSignificantBits() };

        }
    }

    void readBundle(final NodeId nodeId, InputStream istream, Visitor<NodeDescription> visitor, boolean onlyReferenceable) throws IOException {
        BundleBinding bundleBinding = new BundleBinding(new ErrorHandling(ErrorHandling.IGNORE_MISSING_BLOBS), this, nsIndex, nameIndex, this);
        final NodePropBundle bundle = bundleBinding.readBundle(istream, nodeId);
        if (!onlyReferenceable || bundle.isReferenceable()) {
            visitor.visit(new NodeDescriptionImpl(nodeId, bundle));
        }
    }

    @Override
    public String createId(PropertyId id, int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void put(String blobId, InputStream in, long size) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InputStream get(String blobId) throws Exception {
        return null;
    }

    @Override
    public boolean remove(String blobId) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataRecord getRecordIfStored(DataIdentifier identifier) throws DataStoreException {
        return null;
    }

    @Override
    public DataRecord getRecord(DataIdentifier identifier) throws DataStoreException {
        return null;
    }

    @Override
    public DataRecord addRecord(InputStream stream) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateModifiedDateOnAccess(long before) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int deleteAllOlderThan(long min) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<DataIdentifier> getAllIdentifiers() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void init(String homeDir) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMinRecordLength() {
        return 64 * 1024;
    }

    @Override
    public void close() throws DataStoreException {
    }

    @Override
    public void clearInUse() {
    }

    class NodeDescriptionImpl implements NodeDescription {
        NodeId nodeId;
        NodePropBundle bundle;

        NodeDescriptionImpl(NodeId nodeId, NodePropBundle bundle) {
            this.nodeId = nodeId;
            this.bundle = bundle;
        }

        public UUID getNode() {
            return create(nodeId);
        }

        public UUID getParent() {
            return create(bundle.getParentId());
        }

        public Collection<UUID> getChildren() {
            List<UUID> children = new LinkedList<UUID>();
            for (ChildNodeEntry child : (List<ChildNodeEntry>)bundle.getChildNodeEntries()) {
                children.add(create(child.getId()));
            }
            return children;
        }

        public Collection<UUID> getReferences() {
            List<UUID> references = new LinkedList<UUID>();
            for (PropertyEntry entry : (Collection<PropertyEntry>)bundle.getPropertyEntries()) {
                if (entry.getType() == PropertyType.REFERENCE) {
                    for (InternalValue value : entry.getValues()) {
                        references.add(create(value.getNodeId()));
                    }
                }
            }
            return references;
        }
    }

    @Override
    public int getSize() {
        // fetching the size is too expensive for InnoDB tables
        return -1;
    }
}
