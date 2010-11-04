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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String location = args[0];
        String driver = "org.gjt.mm.mysql.Driver"; // alternative: com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource
        String indices = args[1];
        try {
            Traverse traverse = new Traverse();
            Connection connection = getConnection(driver, location, null, null);
            {
                BundleReader bundleReader = new BundleReader(connection, "VERSION_", true);
                int size = bundleReader.getSize();
                System.err.println("Traversing through "+size+" bundles");
                Iterable<NodeDescription> iterable = Coroutine.<NodeDescription>toIterable(bundleReader, size);
                traverse.checkVersionBundles(iterable);
            }
            {
                BundleReader bundleReader = new BundleReader(connection, "DEFAULT_", false);
                int size = bundleReader.getSize();
                System.err.println("Traversing through "+size+" bundles");
                Iterable<NodeDescription> iterable = Coroutine.<NodeDescription>toIterable(bundleReader, size);
                traverse.checkBundles(iterable);
            }
            {
                ReferencesReader referenceReader = new ReferencesReader(connection);
                Iterable<NodeReference> iterable = Coroutine.<NodeReference>toIterable(referenceReader, referenceReader.getSize());
                traverse.checkReferences(iterable);
            }
            connection.close();
            {
                IndicesReader indicesReader = new IndicesReader(new File(indices));
                Iterable<NodeIndexed> iterable = Coroutine.<NodeIndexed>toIterable(indicesReader);
                Iterable<UUID> corrupted = traverse.checkIndices(iterable);
                /*for(UUID uuid : corrupted) {
                    indicesReader.writeIndex(new File(indices), uuid, null);
                }*/
            }
        } catch (SQLException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (Throwable ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }


    public static Connection getConnection(String driver, String url,
                                           String user, String password) throws RepositoryException,
                                                                                SQLException {
        if (driver != null && driver.length() > 0) {
            try {
                Class<?> d = Class.forName(driver);
                if (javax.naming.Context.class.isAssignableFrom(d)) {
                    // JNDI context
                    Context context = (Context)d.newInstance();
                    DataSource ds = (DataSource)context.lookup(url);
                    if (user == null && password == null) {
                        return ds.getConnection();
                    } else {
                        return ds.getConnection(user, password);
                    }
                } else {
                    try {
                        // Workaround for Apache Derby:
                        // The JDBC specification recommends the Class.forName method without the .newInstance() method call,
                        // but it is required after a Derby 'shutdown'.
                        d.newInstance();
                    } catch (Throwable e) {
                        // Ignore exceptions
                        // There's no requirement that a JDBC driver class has a public default constructor
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new RepositoryException("Could not load class " + driver, e);
            } catch (InstantiationException e) {
                throw new RepositoryException("Could not instantiate context " + driver, e);
            } catch (IllegalAccessException e) {
                throw new RepositoryException("Could not instantiate context " + driver, e);
            } catch (NamingException e) {
                throw new RepositoryException("Naming exception using " + driver + " url: " + url, e);
            }
        }
        return DriverManager.getConnection(url, user, password);
    }

}
