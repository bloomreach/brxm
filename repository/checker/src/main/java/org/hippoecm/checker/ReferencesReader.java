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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
        this.wspName = wspName;
    }

    public void accept(Visitor<NodeReference> visitor) {
        try {
            ResultSet rs = access.getConnectionHelper().exec(access.getNodeReferencesSelectAllSQL(), new String[] {}, false, 0);
            //stmt.clearParameters();
            //stmt.clearWarnings();
            while (rs.next()) {
                int column = 0;
                final NodeId nodeId;
                //if(pm.getStorageModel() == SM_BINARY_KEYS) {
                if (true == false) {
                    byte[] nodeIdBytes = rs.getBytes(++column);
                    nodeId = new NodeId(nodeIdBytes);
                } else {
                    long high = rs.getLong(++column);
                    long low = rs.getLong(++column);
                    nodeId = new NodeId(high, low);
                }
                DataInputStream in;
                if (rs.getMetaData().getColumnType(++column) == Types.BLOB) {
                    in = new DataInputStream(rs.getBlob(column).getBinaryStream());
                } else {
                    in = new DataInputStream(rs.getBinaryStream(column));
                }
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
            }
        } catch (SQLException ex) {
            // FIXME
        } catch (IOException ex) {
            // FIXME
        }
    }

    public int getSize() {
        try {
            return getSize(wspName);
        } catch (SQLException ex) {
            ex.printStackTrace();
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
}
