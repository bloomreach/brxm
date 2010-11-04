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
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;

class ReferencesReader extends DatabaseDelegate<NodeReference> implements Visitable<NodeReference> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    ReferencesReader(Connection connection) {
        super(connection, "DEFAULT_");
    }

    public void accept(Visitor<NodeReference> visitor) {
        try {
            accept(visitor, "DEFAULT_");
        } catch (SQLException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    public void accept(Visitor<NodeReference> visitor, String schemaObjectPrefix) throws SQLException, IOException {
        String nodeReferenceSelectSQL = "select NODE_ID, REFS_DATA from " + schemaObjectPrefix + "REFS";
        PreparedStatement stmt = connection.prepareStatement(nodeReferenceSelectSQL);
        stmt.clearParameters();
        stmt.clearWarnings();
        stmt.execute();
        ResultSet rs = stmt.getResultSet();
        while (rs.next()) {
            byte[] nodeIdBytes = rs.getBytes(1);
            Blob blob = rs.getBlob(2);
            final NodeId nodeId = new NodeId(nodeIdBytes);
            byte[] bytes = getBytes(blob);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
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
    }

    public int getSize() {
        try {
            return getSize("DEFAULT_");
        } catch (SQLException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    private int getSize(String schemaObjectPrefix) throws SQLException{
        String bundleSelectAllSQL = "select COUNT(*) from " + schemaObjectPrefix + "REFS";
        PreparedStatement stmt = connection.prepareStatement(bundleSelectAllSQL);
        stmt.clearParameters();
        stmt.clearWarnings();
        stmt.execute();
        ResultSet rs = stmt.getResultSet();
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            return -1;
        }
    }
}
