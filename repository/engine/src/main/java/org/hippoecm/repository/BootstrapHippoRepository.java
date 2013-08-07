/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BootstrapHippoRepository extends HippoRepositoryImpl {
    /** SVN id placeholder */

    protected final Logger log = LoggerFactory.getLogger(BootstrapHippoRepository.class);

    private JackrabbitRepository backingRepository = null;

    private String location;

    public BootstrapHippoRepository(String location) throws RepositoryException {
        super();
        this.location = location;

        backingRepository = RepositoryImpl.create(RepositoryConfig.create(getClass().getResourceAsStream("BootstrapHippoRepository-repository.xml"), "/dev/null"));
        repository = backingRepository;

        Properties properties = new Properties();
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup(location);
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            try {
                ResultSet rs = stmt.executeQuery("SELECT hippokey, hippoval FROM hippotbl");
                while(rs.next()) {
                    String key = rs.getString(1);
                    String value = rs.getString(2);
                    properties.put(key, value);
                }
                rs.close();
                stmt.close();
            } catch(SQLException ex) {
                log.warn("bootstrap database not found or not accessible, trying to create: " + ex.getMessage());
                stmt.close();
                stmt = conn.createStatement();
                stmt.executeQuery("CREATE TABLE hippotbl ( hippokey VARCHAR(64), hippoval VARCHAR(63) )");
                stmt.close();
                log.info("bootstrap database created, loading default content");
                InputStream is = null;
                try {
                    is = getClass().getResourceAsStream("BootstrapHippoRepository-template.properties");
                    properties.load(is);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            conn.close();
        } catch(IOException ex) {
            log.error("bootstrap database communication problem: "+ex.getMessage());
        } catch(SQLException ex) {
            log.error("bootstrap database access problem: "+ex.getMessage());
        } catch(NamingException ex) {
            log.error("bootstrap database name lookup failure: "+ex.getMessage());
        }
    }

    public static HippoRepository create(String location) throws RepositoryException {
        if(location.startsWith("bootstrap:"))
            location = location.substring("bootstrap:".length());
        BootstrapHippoRepository repository = new BootstrapHippoRepository(location);
        Session session = repository.login();
        HippoRepository pivotRepository = LocalHippoRepository.create(session.getRootNode().getProperty("repository").getString());
        session.logout();
        repository.close();
        return pivotRepository;
    }

    public synchronized void close() {

        if (backingRepository != null) {
            try {
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup(location);
                Connection conn = ds.getConnection();
                {
                    Statement stmt = conn.createStatement();
                    stmt.executeUpdate("DELETE FROM hippotbl");
                    stmt.close();
                }
                conn.close();
            } catch(SQLException ex) {
                log.error("bootstrap database failure: "+ex.getMessage());
            } catch(NamingException ex) {
                log.error("bootstrap database name lookup failure: "+ex.getMessage());
            }

            try {
                backingRepository.shutdown();
                backingRepository = null;
            } catch (Exception ex) {
                // ignore
            }
        }
        repository = null;

        super.close();
    }

}
