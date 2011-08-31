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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.jcr.PropertyType;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.util.BundleBinding;
import org.apache.jackrabbit.core.persistence.util.ErrorHandling;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle.ChildNodeEntry;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle.PropertyEntry;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

class BundleReader extends DatabaseDelegate<NodeDescription> implements Visitable<NodeDescription> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    boolean onlyReferenceable;

    BundleReader(PersistenceManager persistMgr, boolean onlyReferenceable) {
        super(persistMgr);
        this.onlyReferenceable = onlyReferenceable;
    }

    public int getSize() {
        int size = -1;
        try {
            ResultSet rs = access.getConnectionHelper().exec(access.getBundleSelectCountSQL(), new String[] { }, false, 0);
            if (rs.next()) {
                size = rs.getInt(1);
            } else {
                size = -1;
            }
            rs.close();
            return size;
        } catch (SQLException ex) {
            Checker.log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            return 0;
        }
    }

    public void accept(Visitor<NodeDescription> visitor) {
        try {
            int totalSize = getSize();
            int batchSize = access.getBundleBatchSize();
            int incrementSize = (batchSize <= 0 ? totalSize : batchSize);
            for (int position = 0; position < totalSize; position += incrementSize) {
                ResultSet rs;
                rs = access.getConnectionHelper().exec(access.getBundleSelectAllSQL(), new String[] { Integer.toString(position) }, false, 0);
                
                while (rs.next()) {
                    int column = 0;
                    final NodeId nodeId;
                    //if(pm.getStorageModel() == SM_BINARY_KEYS) {
                    if(true == false) {
                        byte[] nodeIdBytes = rs.getBytes(++column);
                        nodeId = new NodeId(nodeIdBytes);
                    } else {
                        long high = rs.getLong(++column);
                        long low = rs.getLong(++column);
                        nodeId = new NodeId(high, low);
                    }
                    InputStream in;
                    if (rs.getMetaData().getColumnType(++column) == Types.BLOB) {
                        in = rs.getBlob(column).getBinaryStream();
                    } else {
                        in = rs.getBinaryStream(column);
                    }
                    if (onlyReferenceable) {
                        readBundle(nodeId, in, visitor, onlyReferenceable);
                    } else {
                        readBundle(nodeId, in, visitor, onlyReferenceable);
                    }
                }
                rs.close();
            }
        } catch (SQLException ex) {
            Checker.log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (IOException ex) {
            Checker.log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }

    void repair(final NodeId nodeId, UUID parent, Collection<UUID> children) throws IOException {
        String schemaObjectPrefix = "DEFAULT_";
        Connection connection = null;
        try {
            String bundleSelectSQL = "select BUNDLE_DATA from " + schemaObjectPrefix + "BUNDLE WHERE NODE_ID = ?";
            PreparedStatement stmt = connection.prepareStatement(bundleSelectSQL);
            stmt.clearParameters();
            stmt.setBytes(1, nodeId.getRawBytes());
            stmt.clearWarnings();
            stmt.execute();
            ResultSet rs = stmt.getResultSet();
            rs.next();
            Blob blob = rs.getBlob(1);
            byte[] bytes = getBytes(blob);
            DataInputStream istream = new DataInputStream(new ByteArrayInputStream(bytes));
            BundleBinding bundleBinding = new BundleBinding(new ErrorHandling(ErrorHandling.IGNORE_MISSING_BLOBS), null, nsIndex, nameIndex, null);
            NodePropBundle bundle = bundleBinding.readBundle(istream, nodeId);
            bundle.setParentId(NodeId.valueOf(parent.toString()));
            for(Iterator<ChildNodeEntry> iter = ((List<ChildNodeEntry>) bundle.getChildNodeEntries()).iterator(); iter.hasNext(); ) {
                ChildNodeEntry childNodeEntry = (ChildNodeEntry) iter.next();
                if(!children.contains(create(childNodeEntry.getId()))) {
                    iter.remove();
                }
            }
            /*
            for(UUID child : children) {
                NodeId childNodeId = NodeId.valueOf(child.toString());
                if(!bundle.getChildNodeEntries().contains(childNodeId)) {
                    bundle.addChildNodeEntry("lost", nodeId);
                }
            }
            */
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            bundleBinding.writeBundle(new DataOutputStream(ostream), bundle);
            String bundleUpdateSQL = "update " + schemaObjectPrefix + "BUNDLE SET BUNDLE_DATA = ? WHERE NODE_ID = ?";
            PreparedStatement stmt2 = connection.prepareStatement(bundleUpdateSQL);
            stmt2.clearParameters();
            blob.truncate(0);
            blob.setBytes(0, ostream.toByteArray());
            stmt2.setBlob(1, blob);
            stmt2.setBytes(2, nodeId.getRawBytes());
            stmt2.clearWarnings();
            stmt2.executeUpdate();
        } catch (SQLException ex) {
            Checker.log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } catch (IOException ex) {
            Checker.log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }

    void readBundle(final NodeId nodeId, InputStream istream, Visitor<NodeDescription> visitor, boolean onlyReferenceable) throws IOException {
        BundleBinding bundleBinding = new BundleBinding(new ErrorHandling(ErrorHandling.IGNORE_MISSING_BLOBS), null, nsIndex, nameIndex, null);
        final NodePropBundle bundle = bundleBinding.readBundle(istream, nodeId);
        if (!onlyReferenceable || bundle.isReferenceable()) {
            visitor.visit(new NodeDescription() {
                public UUID getNode() {
                    return create(nodeId);
                }

                public UUID getParent() {
                    return create(bundle.getParentId());
                }

                public Collection<UUID> getChildren() {
                    List<UUID> children = new LinkedList<UUID>();
                    for (ChildNodeEntry child : (List<ChildNodeEntry>)bundle.getChildNodeEntries()) {
                        if(children.contains(create(child.getId()))) {
                            Thread.currentThread().dumpStack();
                        }
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
            });
        }
    }
}
