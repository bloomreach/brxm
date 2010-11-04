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
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.jcr.PropertyType;
import org.apache.jackrabbit.core.id.NodeId;
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
    int batchSize=1000000;

    BundleReader(Connection connection, String schemaObjectPrefix, boolean onlyReferenceable) {
        super(connection, schemaObjectPrefix);
        this.onlyReferenceable = onlyReferenceable;
    }

    public int getSize() {
        int size = -1;
        try {
            String bundleCountSQL = "select COUNT(*) from " + schemaObjectPrefix + "BUNDLE";
            Statement stmt = connection.createStatement();
            stmt.execute(bundleCountSQL);
            ResultSet rs = stmt.getResultSet();
            if (rs.next()) {
                size = rs.getInt(1);
            } else {
                size = -1;
            }
            rs.close();
            stmt.close();
            return size;
        } catch (SQLException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
            return 0;
        }
    }

    public void accept(Visitor<NodeDescription> visitor) {
        try {
            int totalSize = getSize();
            for (int position = 0; position < totalSize; position += batchSize) {
                String bundleSelectAllSQL = "select NODE_ID, BUNDLE_DATA from " + schemaObjectPrefix + "BUNDLE LIMIT " + position + "," + batchSize;
                Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                stmt.execute(bundleSelectAllSQL);
                ResultSet rs = stmt.getResultSet();
                while (rs.next()) {
                    byte[] nodeIdBytes = rs.getBytes(1);
                    Blob blob = rs.getBlob(2);
                    final NodeId nodeId = new NodeId(nodeIdBytes);
                    byte[] bytes = getBytes(blob);
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
                    if (onlyReferenceable) {
                        readBundle(nodeId, in, visitor, onlyReferenceable);
                    } else {
                        readBundle(nodeId, in, visitor, onlyReferenceable);
                    }
                }
                rs.close();
                stmt.close();
            }
        } catch (SQLException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    void repair(final NodeId nodeId, UUID parent, Collection<UUID> children) throws IOException {
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
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    void readBundle(final NodeId nodeId, DataInputStream istream, Visitor<NodeDescription> visitor, boolean onlyReferenceable) throws IOException {
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

    void readBundle(final NodeId nodeId, DataInputStream in, Visitor<NodeDescription> visitor) throws IOException {
                int version;
                // read version and primary type...special handling
                int index = in.readInt();
                // get version
                version = (index >> 24) & 0xff;
                index &= 0x00ffffff;
                String uri = nsIndex.indexToString(index);
                String local = nameIndex.indexToString(in.readInt());
                Name nodeTypeName = NameFactoryImpl.getInstance().create(uri, local);
                log.debug("Serialzation Version: " + version);
                log.debug("NodeTypeName: " + nodeTypeName);
                final NodeId parentId = readID(in);
                log.debug("ParentUUID: " + parentId);
                String definitionId = in.readUTF();
                log.debug("DefinitionId: " + definitionId);
                Name mixinName = readIndexedQName(in);
                while (mixinName != null) {
                    log.debug("MixinTypeName: " + mixinName);
                    mixinName = readIndexedQName(in);
                }
                Name propName = readIndexedQName(in);
                final Collection<UUID> refsIds = new LinkedList<UUID>();
                while (propName != null) {
                    log.debug("PropertyName: " + propName);
                    if (!checkPropertyState(in, refsIds)) {
                        throw new IOException();
                    }
                    propName = readIndexedQName(in);
                }
                boolean hasUUID = in.readBoolean();
                log.debug("hasUUID: " + hasUUID);
                final Collection<UUID> cneIds = new LinkedList<UUID>();
                for (;;) {
                    final NodeId cneId = readID(in);
                    if (cneId == null) {
                        break;
                    }
                    Name cneName = readQName(in);
                    log.debug("ChildNodentry: " + cneId + ":" + cneName);
                    cneIds.add(create(cneId));
                }
                if (version >= 1) {
                    short modCount = in.readShort();
                    log.debug("modCount: " + modCount);
                }
                // read shared set, since version 2.0
                Set<NodeId> sharedSet = new HashSet<NodeId>();
                if (version >= 2) {
                    // shared set (list of parent uuids)
                    NodeId sharedParentId = readID(in);
                    while (sharedParentId != null) {
                        sharedSet.add(sharedParentId);
                        sharedParentId = readID(in);
                    }
                }
                visitor.visit(new NodeDescription() {
                    public UUID getNode() {
                        return create(nodeId);
                    }
                    public UUID getParent() {
                        return create(parentId);
                    }
                    public Collection<UUID> getChildren() {
                        return cneIds;
                    }
                    public Collection<UUID> getReferences() {
                        return refsIds;
                    }
                });
    }

    private boolean checkPropertyState(DataInputStream in, Collection<UUID> refsIds) {
        final int BINARY_IN_BLOB_STORE = -1;
        final int BINARY_IN_DATA_STORE = -2;
        int type;
        try {
            type = in.readInt();
            short modCount = (short) ((type >> 16) | 0xffff);
            type &= 0xffff;
            log.debug("  PropertyType: " + PropertyType.nameFromValue(type));
            log.debug("  ModCount: " + modCount);
        } catch (IOException e) {
            log.error("Error while reading property type: " + e);
            return false;
        }
        try {
            boolean isMV = in.readBoolean();
            log.debug("  MultiValued: " + isMV);
        } catch (IOException e) {
            log.error("Error while reading multivalued: " + e);
            return false;
        }
        try {
            String defintionId = in.readUTF();
            log.debug("  DefinitionId: " + defintionId);
        } catch (IOException e) {
            log.error("Error while reading definition id: " + e);
            return false;
        }

        int count;
        try {
            count = in.readInt();
            log.debug("  num values: " + count);
        } catch (IOException e) {
            log.error("Error while reading number of values: " + e);
            return false;
        }
        for (int i = 0; i < count; i++) {
            switch (type) {
                case PropertyType.BINARY:
                    int size;
                    try {
                        size = in.readInt();
                        log.debug("  binary size: " + size);
                    } catch (IOException e) {
                        log.error("Error while reading size of binary: " + e);
                        return false;
                    }
                    if (size == BINARY_IN_DATA_STORE) {
                        try {
                            String s = in.readUTF();
                            // truncate log output
                            if (s.length() > 80) {
                                s = s.substring(80) + "...";
                            }
                            log.debug("  global data store id: " + s);
                        } catch (IOException e) {
                            log.error("Error while reading blob id: " + e);
                            return false;
                        }
                    } else if (size == BINARY_IN_BLOB_STORE) {
                        try {
                            String s = in.readUTF();
                            log.debug("  blobid: " + s);
                        } catch (IOException e) {
                            log.error("Error while reading blob id: " + e);
                            return false;
                        }
                    } else {
                        // short values into memory
                        byte[] data = new byte[size];
                        try {
                            in.readFully(data);
                            log.debug("  binary: " + data.length + " bytes");
                        } catch (IOException e) {
                            log.error("Error while reading inlined binary: " + e);
                            return false;
                        }
                    }
                    break;
                case PropertyType.DOUBLE:
                    try {
                        double d = in.readDouble();
                        log.debug("  double: " + d);
                    } catch (IOException e) {
                        log.error("Error while reading double value: " + e);
                        return false;
                    }
                    break;
                /*case PropertyType.DECIMAL:
                try {
                BigDecimal d = readDecimal(in);
                log.debug("  decimal: " + d);
                } catch (IOException e) {
                log.error("Error while reading decimal value: " + e);
                return false;
                }
                break;*/
                case PropertyType.LONG:
                    try {
                        double l = in.readLong();
                        log.debug("  long: " + l);
                    } catch (IOException e) {
                        log.error("Error while reading long value: " + e);
                        return false;
                    }
                    break;
                case PropertyType.BOOLEAN:
                    try {
                        boolean b = in.readBoolean();
                        log.debug("  boolean: " + b);
                    } catch (IOException e) {
                        log.error("Error while reading boolean value: " + e);
                        return false;
                    }
                    break;
                case PropertyType.NAME:
                    try {
                        Name name = readQName(in);
                        log.debug("  name: " + name);
                    } catch (IOException e) {
                        log.error("Error while reading name value: " + e);
                        return false;
                    }
                    break;
                /*case PropertyType.WEAKREFERENCE:*/
                case PropertyType.REFERENCE:
                    try {
                        NodeId id = readID(in);
                        refsIds.add(create(id));
                        log.debug("  reference: " + id);
                    } catch (IOException e) {
                        log.error("Error while reading reference value: " + e);
                        return false;
                    }
                    break;
                default:
                    // because writeUTF(String) has a size limit of 64k,
                    // Strings are serialized as <length><byte[]>
                    int len;
                    try {
                        len = in.readInt();
                        log.debug("  size of string value: " + len);
                    } catch (IOException e) {
                        log.error("Error while reading size of string value: " + e);
                        return false;
                    }
                    try {
                        byte[] bytes = new byte[len];
                        in.readFully(bytes);
                        String s = new String(bytes, "UTF-8");
                        // truncate log output
                        if (s.length() > 80) {
                            s = s.substring(80) + "...";
                        }
                        log.debug("  string: " + s);
                    } catch (IOException e) {
                        log.error("Error while reading string value: " + e);
                        return false;
                    }
            }
        }
        return true;
    }
}
