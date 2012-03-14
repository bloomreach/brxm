package org.apache.jackrabbit.core.persistence.pool;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;

public class Access {
    private BundleDbPersistenceManager pm;

    public Access(BundleDbPersistenceManager pm) {
        this.pm = pm;
    }

    public String getNodeReferencesSelectAllSQL() {
        return "select * from " + pm.schemaObjectPrefix + "REFS";
    }

    public String getNodeReferencesSelectCountSQL() {
        return "select COUNT(*) from " + pm.schemaObjectPrefix + "REFS";
    }

    public NodePropBundle loadBundle(NodeId nodeId) throws ItemStateException {
        return pm.loadBundle(nodeId);
    }

    public void storeBundle(NodePropBundle bundle) throws ItemStateException {
        pm.storeBundle(bundle);
    }

    public String getBundleDeleteSQL() {
        return pm.bundleDeleteSQL;
    }

    private byte[] getBytes(UUID node) {
        byte[] bytes = new byte[16];
        bytes[0] = (byte)((node.getMostSignificantBits() >> 56) & 0xff);
        bytes[1] = (byte)((node.getMostSignificantBits() >> 48) & 0xff);
        bytes[2] = (byte)((node.getMostSignificantBits() >> 40) & 0xff);
        bytes[3] = (byte)((node.getMostSignificantBits() >> 32) & 0xff);
        bytes[4] = (byte)((node.getMostSignificantBits() >> 24) & 0xff);
        bytes[5] = (byte)((node.getMostSignificantBits() >> 16) & 0xff);
        bytes[6] = (byte)((node.getMostSignificantBits() >> 8) & 0xff);
        bytes[7] = (byte)((node.getMostSignificantBits()) & 0xff);
        bytes[8] = (byte)((node.getLeastSignificantBits() >> 56) & 0xff);
        bytes[9] = (byte)((node.getLeastSignificantBits() >> 48) & 0xff);
        bytes[10] = (byte)((node.getLeastSignificantBits() >> 40) & 0xff);
        bytes[11] = (byte)((node.getLeastSignificantBits() >> 32) & 0xff);
        bytes[12] = (byte)((node.getLeastSignificantBits() >> 24) & 0xff);
        bytes[13] = (byte)((node.getLeastSignificantBits() >> 16) & 0xff);
        bytes[14] = (byte)((node.getLeastSignificantBits() >> 8) & 0xff);
        bytes[15] = (byte)((node.getLeastSignificantBits()) & 0xff);
        return bytes;
    }

    public boolean destroyBundle(UUID nodeId) throws SQLException {
        Object[] arguments;
        if (getStorageModelBinaryKeys()) {
            byte[][] bytes = new byte[1][];
            bytes[0] = getBytes(nodeId);
            arguments = bytes;
        } else {
            arguments = new Long[] {nodeId.getMostSignificantBits(), nodeId.getLeastSignificantBits()};
        }
        int count = getConnectionHelper().update(getBundleDeleteSQL(), arguments);
        return count >= 1;
    }

    public String getBundleSelectAllSQL() {
        String sql = pm.bundleSelectAllIdsSQL;
        sql = sql.replace("NODE_ID_HI, NODE_ID_LO", "*");
        sql = sql.replace("NODE_ID", "*");
        if("derby".equals(pm.getDatabaseType())) {
            sql += " offset ? rows fetch next "+getBundleBatchSize()+" rows only";
        } else if("mysql".equals(pm.getDatabaseType())) {
            sql += " LIMIT ?, " + getBundleBatchSize();
        }
        // for MySQL we would like have to add on the createStatement: ResultSet.TYPE_FORWARD_ONLY and ResultSet.CONCUR_READ_ONLY
        return sql;
    }

    public int getBundleBatchSize() {
        if("derby".equals(pm.getDatabaseType())) {
            return 16;
        } else if("mysql".equals(pm.getDatabaseType())) {
            return 1000;
        } else {
            return 0;
        }
    }

    public String getBundleSelectCountSQL() {
        String sql = pm.bundleSelectAllIdsSQL;
        sql = sql.replace("NODE_ID_HI, NODE_ID_LO", "COUNT(*)");
        sql = sql.replace("NODE_ID", "COUNT(*)");
        return sql;
    }

    public boolean getStorageModelBinaryKeys() {
        return pm.getStorageModel() == pm.SM_BINARY_KEYS;
    }

    public ConnectionHelper getConnectionHelper() {
        return pm.conHelper;
    }

    public static void close(PersistenceManager persistMgr) {
        try {
            persistMgr.close();
        } catch (Exception ex) {
        }
        if (persistMgr instanceof BundleDbPersistenceManager && "derby".equals(((BundleDbPersistenceManager)persistMgr).getDatabaseType())) {
            try {
                DriverManager.getConnection("jdbc:derby:;shutdown=true;deregister=true");
            } catch (SQLException e) {
                // a shutdown command always raises a SQLException
            }
        }
    }
}
