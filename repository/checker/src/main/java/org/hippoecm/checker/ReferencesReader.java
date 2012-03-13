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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.UUID;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.PersistenceManager;

class ReferencesReader extends DatabaseDelegate<NodeReference> implements Visitable<NodeReference> {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    String wspName;

    ReferencesReader(PersistenceManager persistMgr) {
        super(persistMgr);
    }

    public void accept(Visitor<NodeReference> visitor) {
        try {
            ResultSet rs = access.getConnectionHelper().exec(access.getNodeReferencesSelectAllSQL(), new String[] {}, false, 0);
            //stmt.clearParameters();
            //stmt.clearWarnings();
            while (rs.next()) {
                Map.Entry<NodeId, InputStream> bundle = readEntry(rs);
                final NodeId nodeId = bundle.getKey();
                InputStream istream = bundle.getValue();
                if (istream != null) {
                    DataInputStream in = new DataInputStream(istream);
                    int count = in.readInt();   // count
                    for (int i = 0; i < count; i++) {
                        final PropertyId propertyId = PropertyId.valueOf(in.readUTF());
                        visitor.visit(new NodeReference() {
                            public UUID getTarget() {
                                return create(nodeId);
                            }

                            public UUID getSource() {
                                return create(propertyId.getParentId());
                            }
                            // propertyId.getParentId(), "{" + propertyId.getName().getNamespaceURI() + "}" + propertyId.getName().getLocalName()
                        });
                    }
                } else {
                    // FIXME
                }
            }
        } catch (SQLException ex) {
            Checker.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } catch (IOException ex) {
            Checker.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } catch (Throwable ex) {
            Checker.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
    }

    public int getSize() {
        try {
            return getSize(wspName);
        } catch (SQLException ex) {
            Checker.log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            return 0;
        }
    }

    private int getSize(String schemaObjectPrefix) throws SQLException {
        int size;
        ResultSet rs = access.getConnectionHelper().exec(access.getNodeReferencesSelectCountSQL(), new String[] { }, false, 0);
        //stmt.clearParameters();
        //stmt.clearWarnings();
        //stmt.execute();
        if (rs.next()) {
            size = rs.getInt(1);
        } else {
            size = -1;
        }
        rs.close();
        return size;
    }
    
    public void repair(UUID node) throws SQLException {
        Thread.currentThread().dumpStack();
        // FIXME
    }
}
