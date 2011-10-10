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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

class BundleReader extends DatabaseDelegate<NodeDescription> implements Visitable<NodeDescription> {;
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    boolean onlyReferenceable;
    Repair repair;

    BundleReader(PersistenceManager persistMgr, boolean onlyReferenceable, Repair repair) {
        super(persistMgr);
        this.onlyReferenceable = onlyReferenceable;
        this.repair = repair;
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
                rs = access.getConnectionHelper().exec(access.getBundleSelectAllSQL(), new Integer[] {new Integer(position)}, false, 0);
                while (rs.next()) {
                    try {
                        Map.Entry<NodeId, InputStream> bundle = readEntry(rs);
                        final NodeId nodeId = bundle.getKey();
                        InputStream in = bundle.getValue();
                        if(in != null) {
                            try {
                                readBundle(nodeId, in, visitor, onlyReferenceable);
                            } catch(IOException ex) {
                                repair.removeNode(Repair.RepairStatus.PENDING, create(nodeId));
                            }
                        } else {
                            repair.removeNode(Repair.RepairStatus.PENDING, create(nodeId));
                        }
                    } catch (Throwable ex) {
                        rs.deleteRow();
                    }
                }
                rs.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.err);
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
