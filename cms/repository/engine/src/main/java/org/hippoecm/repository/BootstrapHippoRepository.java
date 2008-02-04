/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.servicing.ServicingDecoratorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BootstrapRepository extends HippoRepositoryImpl {
    /** SVN id placeholder */
    private final static String SVN_ID = "$Id$";

    protected final Logger log = LoggerFactory.getLogger(BootstrapRepository.class);

    private JackrabbitRepository backingRepository = null;

    private String location;

    public BootstrapRepository(String location) throws RepositoryException {
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
                log.warn("bootstrap database not found or not accessible, trying to create: "+ex.getMessage());
                stmt.close();
                stmt = conn.createStatement();
                stmt.executeQuery("CREATE TABLE hippotbl ( hippokey VARCHAR(64), hippoval VARCHAR(63) )");
                stmt.close();
                log.info("bootstrap database created, loading default content");
                properties.load(getClass().getResourceAsStream("BootstrapHippoRepository-template.properties"));
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
        BootstrapRepository repository = new BootstrapRepository(location);
        Session session = repository.login();
        HippoRepository pivotRepository = LocalHippoRepository.create(session.getRootNode().getProperty("repository").getString());
        session.logout();
        repository.close();
        return pivotRepository;
    }

    public synchronized void close() {
        Session session = null;

        if (backingRepository != null) {

            Properties properties = new Properties();

            try {
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup(location);
                Connection conn = ds.getConnection();
                {
                    Statement stmt = conn.createStatement();
                    stmt.executeUpdate("DELETE FROM hippotbl");
                    stmt.close();
                } {
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO hippotbl ( hippokey, hippoval ) VALUES ( ?, ? )");
                    for(Map.Entry entry : properties.entrySet()) {
                        stmt.setString(1, (String) entry.getKey());
                        stmt.setString(2, (String) entry.getValue());
                        stmt.executeUpdate();
                        stmt.clearParameters();
                    }
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
