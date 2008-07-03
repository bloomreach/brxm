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
import java.rmi.RemoteException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.*;
import static org.junit.Assert.*;

import org.apache.jackrabbit.spi.RepositoryService;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.hippoecm.repository.TestCase;
import org.hippoecm.testutils.history.HistoryWriter;

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

    @Test
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
            long duration = test(session);
            HistoryWriter.write("traversal", Long.toString(duration), "ms");
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
            long duration = test(session);
            HistoryWriter.write("traversal", Long.toString(duration), "ms");
        } finally {
            if (session != null) {
                session.logout();
            }
            if (repository != null) {
                repository.shutdown();
            }
        }
    }

    @Test
    public void testNew() throws Exception {
        tearDown(true);
        HippoRepositoryServer backgroundServer = null;
        HippoRepository server = null;
        Session session = null;
        long duration;
        try {
            final org.apache.jackrabbit.spi.RepositoryService repoService;
            backgroundServer = new HippoRepositoryServer();
            backgroundServer.run(true);
            Thread.sleep(3000);
            server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            org.apache.jackrabbit.spi2jcr.BatchReadConfig cfg = new org.apache.jackrabbit.spi2jcr.BatchReadConfig();
            //cfg.setDepth(, 2);
            repoService = new org.apache.jackrabbit.spi2jcr.RepositoryServiceImpl(server.getRepository(), cfg);
            Repository repo3 = org.apache.jackrabbit.jcr2spi.RepositoryImpl.create(new org.apache.jackrabbit.jcr2spi.config.RepositoryConfig() {
                         public RepositoryService getRepositoryService() throws RepositoryException {
                             return repoService;
                         }
                         public String getDefaultWorkspaceName() {
                             return "default";
                         }
                         public org.apache.jackrabbit.jcr2spi.config.CacheBehaviour getCacheBehaviour() {
                             return org.apache.jackrabbit.jcr2spi.config.CacheBehaviour.INVALIDATE;
                         }
                });
            session = backgroundServer.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            build(session, content); 
            session.save();
            duration = test(session);
            HistoryWriter.write("same", Long.toString(duration), "ms");
            session.logout();

            session = backgroundServer.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            duration = test(session);
            HistoryWriter.write("local", Long.toString(duration), "ms");
            session.logout();

            session = server.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            duration = test(session);
            HistoryWriter.write("remote", Long.toString(duration), "ms");
            session.logout();

            session = repo3.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            duration = test(session);
            HistoryWriter.write("cached", Long.toString(duration), "ms");
            session.logout();
        } finally {
            if (session != null) {
                session.logout();
            }
            if (backgroundServer != null) {
                backgroundServer.close();
            }
        }
    }

    @Test
    public void testLocal() throws Exception {
        build(session, content);
        long duration = test(session);
        HistoryWriter.write("traversal", Long.toString(duration), "ms");
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
            long duration = test(session);
            HistoryWriter.write("traversal", Long.toString(duration), "ms");
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
        }
    }

    private long test(Session session) throws RepositoryException {
        int count = 100;
        long tAfter, tBefore = System.currentTimeMillis();
        Node root = session.getRootNode();
        for(int i=0; i<count; i++) {
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
