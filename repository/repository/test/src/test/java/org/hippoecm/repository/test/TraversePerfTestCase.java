/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.test;

import java.io.InputStream;
import java.rmi.Remote;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.spi.RepositoryService;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.hippoecm.repository.TestCase;
import org.hippoecm.testutils.history.HistoryWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/*
import org.apache.jackrabbit.spi.rmi.client.ClientRepositoryService;
import org.apache.jackrabbit.spi.rmi.server.ServerRepositoryService;
import org.apache.jackrabbit.spi.rmi.remote.RemoteRepositoryService;
*/

public class TraversePerfTestCase extends TestCase {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private String[] content = {
        "/test",                                       "nt:unstructured",
        "/test/aap",                                   "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot",                                  "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/mies",                                  "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/wim",                              "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus",                              "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet",                          "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun",                     "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur",                "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/gijs",                     "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/lam",            "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/kees",                "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/bok",            "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide",          "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/does",           "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide/hok",      "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide/schapen",  "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide/hok/duif", "nt:unstructured",
        "foo", "",
        "bar", ""
    };

    @Override
    @Before
    public void setUp() throws Exception {
        external = null;
        super.setUp(true);
        Node node, root = session.getRootNode();
        if(root.hasNode("test")) {
            root.getNode("test").remove();
        }
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if(session != null) {
            session.refresh(false);
            if (session.getRootNode().hasNode("test")) {
                session.getRootNode().getNode("test").remove();
            }
        }
        super.tearDown(true);
    }

    @Ignore
    public void testBase() throws Exception {
        tearDown(true);
        org.apache.jackrabbit.core.RepositoryImpl repository = null;
        org.apache.jackrabbit.core.config.RepositoryConfig repoConfig = null;
        InputStream config = getClass().getResourceAsStream("jackrabbit.xml");
        String path = ".";
        Session session = null;
        try {
            repoConfig = org.apache.jackrabbit.core.config.RepositoryConfig.create(config, path);
            repository = org.apache.jackrabbit.core.RepositoryImpl.create(repoConfig);
            session = repository.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            build(session, content);
            session.save();
            session.logout();
            session = repository.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            long duration = test(session, 100);
            HistoryWriter.write("traversal", Double.toString(duration/100.0), "ms");
        } finally {
            if (session != null) {
                session.logout();
            }
            if (repository != null) {
                repository.shutdown();
            }
        }
    }

    @Ignore
    public void testSPI() throws Exception {
        tearDown(true);
        org.apache.jackrabbit.core.RepositoryImpl repository = null;
        org.apache.jackrabbit.core.config.RepositoryConfig repoConfig = null;
        InputStream config = getClass().getResourceAsStream("jackrabbit.xml");
        String path = ".";
        Session session = null;
        try {
            repoConfig = org.apache.jackrabbit.core.config.RepositoryConfig.create(config, path);
            repository = org.apache.jackrabbit.core.RepositoryImpl.create(repoConfig);
            final org.apache.jackrabbit.spi.RepositoryService repoService;
            repoService = new org.apache.jackrabbit.spi2jcr.RepositoryServiceImpl(repository, new org.apache.jackrabbit.spi2jcr.BatchReadConfig());

            Repository repo3 = org.apache.jackrabbit.jcr2spi.RepositoryImpl.create(new org.apache.jackrabbit.jcr2spi.config.RepositoryConfig() {
                         public RepositoryService getRepositoryService() throws RepositoryException {
                             return repoService;
                         }
                         public String getDefaultWorkspaceName() {
                             return "default";
                         }
                         public org.apache.jackrabbit.jcr2spi.config.CacheBehaviour getCacheBehaviour() {
                             return org.apache.jackrabbit.jcr2spi.config.CacheBehaviour.OBSERVATION;
                         }
                });
            session = repo3.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            build(session, content);
            session.save();
            session.logout();
            session = repo3.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            long duration = test(session, 100);
            HistoryWriter.write("traversal", Double.toString(duration/100.0), "ms");
        } finally {
            if (session != null) {
                session.logout();
            }
            if (repository != null) {
                repository.shutdown();
            }
        }
    }

    class MyConfig extends org.apache.jackrabbit.spi2jcr.BatchReadConfig {
        public int getDepth(org.apache.jackrabbit.spi.Name ntName) {
            return 100;
        }
    }

    /*
    @Ignore
    public void testNew() throws Exception {
        //tearDown(true);
        //HippoRepositoryServer backgroundServer = null;
        //HippoRepository server = null;
        Session session = null;
        long duration;
        try {
            final org.apache.jackrabbit.spi.RepositoryService repoService;
            //backgroundServer = new HippoRepositoryServer();
            //backgroundServer.run(true);
            //Thread.sleep(3000);
            //server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            org.apache.jackrabbit.spi2jcr.BatchReadConfig cfg = new org.apache.jackrabbit.spi2jcr.BatchReadConfig();
            //org.apache.jackrabbit.spi2jcr.BatchReadConfig cfg = new MyConfig();
            cfg.setDepth(org.apache.jackrabbit.spi.commons.name.NameFactoryImpl.getInstance().create("internal", "root"), -1);
            cfg.setDepth(org.apache.jackrabbit.spi.commons.name.NameFactoryImpl.getInstance().create("http://www.jcp.org/jcr/nt/1.0", "unstructured"), -1);
            cfg.setDepth(org.apache.jackrabbit.spi.commons.name.NameFactoryImpl.getInstance().create("nt", "unstructured"), -1);
            repoService = new org.apache.jackrabbit.spi2jcr.RepositoryServiceImpl(server.getRepository(), cfg);

            ServerRepositoryService serverService = new ServerRepositoryService(repoService);
            //Registry registry = LocateRegistry.getRegistry();
            Registry registry = LocateRegistry.createRegistry(1099);
            System.setProperty("java.rmi.server.useCodebaseOnly", "true");
            registry.bind("hipporepository", serverService);
            RemoteRepositoryService remoteService = (RemoteRepositoryService) Naming.lookup("rmi://localhost:1099/hipporepository");
            final ClientRepositoryService clientService = new ClientRepositoryService(remoteService);

            Repository repo3 = org.apache.jackrabbit.jcr2spi.RepositoryImpl.create(new org.apache.jackrabbit.jcr2spi.config.RepositoryConfig() {
                         public RepositoryService getRepositoryService() throws RepositoryException {
                             return clientService; // return repoService;
                         }
                         public String getDefaultWorkspaceName() {
                             return "default";
                         }
                         public org.apache.jackrabbit.jcr2spi.config.CacheBehaviour getCacheBehaviour() {
                             return org.apache.jackrabbit.jcr2spi.config.CacheBehaviour.OBSERVATION;
                         }
                });
            //session = backgroundServer.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            session = server.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            build(session, content); 
            session.save();
            duration = test(session, 1);
            HistoryWriter.write("direct1", Double.toString(duration/1.0), "ms");
            duration = test(session, 100);
            HistoryWriter.write("direct100", Double.toString(duration/100.0), "ms");
            duration = test(session, 10000);
            HistoryWriter.write("direct10000", Double.toString(duration/10000.0), "ms");
            session.logout();

            / *
            //session = backgroundServer.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            session = server.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            duration = test(session);
            HistoryWriter.write("local", Double.toString(duration), "ms");
            session.logout();

            session = server.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            duration = test(session);
            HistoryWriter.write("remote", Double.toString(duration), "ms");
            session.logout();
            * /

            session = repo3.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            duration = test(session, 1);
            HistoryWriter.write("spi1", Double.toString(duration/1.0), "ms");
            duration = test(session, 100);
            HistoryWriter.write("spi100", Double.toString(duration/100.0), "ms");
            duration = test(session, 10000);
            HistoryWriter.write("spi10000", Double.toString(duration/10000.0), "ms");

            session.logout();
        } catch(Exception ex) {
            System.err.println("================================================================================");
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
            for(Throwable e = ex.getCause(); e != null; e = e.getCause()) {
                System.err.println("--------------------------------------------------------------------------------");
                System.err.println(e.getClass().getName()+": "+e.getMessage());
                e.printStackTrace(System.err);
            }
            System.err.println("================================================================================");
            throw ex;
        } finally {
            if (session != null) {
                session.logout();
            }
            //if (backgroundServer != null) {
            //    backgroundServer.close();
            //}
        }
    }
    */

    @Test
    public void testLocal() throws Exception {
        build(session, content);
        long duration = test(session, 100);
        HistoryWriter.write("traversal", Double.toString(duration/100.0), "ms");
    }

    @Test
    public void testRemote() throws Exception {
        tearDown();
        HippoRepositoryServer backgroundServer = null;
        HippoRepository server = null;
        Session session = null;
        try {
            backgroundServer = new HippoRepositoryServer();
            backgroundServer.run(true);
            Thread.sleep(3000);
            server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            build(session, content);
            session.save();
            session.logout();
            session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            long duration = test(session, 100);
            HistoryWriter.write("traversal", Double.toString(duration/100.0), "ms");
        } finally {
            if (session != null) {
                session.logout();
            }
            if (server != null) {
                server.close();
            }
            if (backgroundServer != null) {
                backgroundServer.close();
            }
            Thread.sleep(3000);
        }
    }

    @Test
    public void testSPIRemote() throws Exception {
        tearDown(true);
        HippoRepositoryServer backgroundServer = null;
        HippoRepository server = null;
        Session session = null;
        try {
            backgroundServer = new HippoRepositoryServer("spi://localhost:1099/hipporepository");
            backgroundServer.run("spi://localhost:1099/hipporepository", true);
            Thread.sleep(3000);
            server = HippoRepositoryFactory.getHippoRepository("spi://localhost:1099/hipporepository");
            session = backgroundServer.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            build(session, content);
            session.save();
            session.logout();
            session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            long duration = test(session, 100);
            HistoryWriter.write("traversal", Double.toString(duration/100.0), "ms");
        } finally {
            if (session != null) {
                session.logout();
            }
            if (server != null) {
                server.close();
            }
            if (backgroundServer != null) {
                backgroundServer.close();
            }
            Thread.sleep(3000);
        }
    }

    private long test(Session session, int count) throws RepositoryException {
        Node root = session.getRootNode();
        long tAfter, tBefore = System.currentTimeMillis();
        for(int i=0; i<count; i++) {
            //if(i==1)
            //tBefore = System.currentTimeMillis();
            Node node = root;
            StringTokenizer st = new StringTokenizer("test/noot/zus/jet/teun/vuur/weide/hok/duif","/");
            while(st.hasMoreTokens()) {
                node = node.getNode(st.nextToken());
                for(PropertyIterator iter = node.getProperties(); iter.hasNext(); ) {
                    Property property = iter.nextProperty();
                    property.getString();
                }
            }
        }
        tAfter = System.currentTimeMillis();
        return tAfter - tBefore;
    }
}
