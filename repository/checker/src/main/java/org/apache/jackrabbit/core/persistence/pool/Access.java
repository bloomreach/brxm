package org.apache.jackrabbit.core.persistence.pool;

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
    public ConnectionHelper getConnectionHelper() {
        return pm.conHelper;
    }
}
