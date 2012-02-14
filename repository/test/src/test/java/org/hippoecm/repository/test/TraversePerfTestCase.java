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
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoNode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;

public class TraversePerfTestCase extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private String[] content = {
        "/test", "nt:unstructured",
        "/test/aap", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/mies", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/wim", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/gijs", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/lam", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/kees", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/bok", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/does", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide/hok", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide/schapen", "nt:unstructured",
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
        HippoRepositoryFactory.setDefaultRepository((String)null);
        super.setUp(true);
        Node root = session.getRootNode();
        if (root.hasNode("test")) {
            root.getNode("test").remove();
        }
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (session != null) {
            session.refresh(false);
            if (session.getRootNode().hasNode("test")) {
                session.getRootNode().getNode("test").remove();
            }
        }
        super.tearDown();
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
            System.out.println("traversal "+Double.toString(duration / 100.0) + "ms");
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
    public void testLocal() throws Exception {
        build(session, content);
        session.save();
        long duration = test(session, 100);
        System.out.println("traversal " + Double.toString(duration / 100.0) + "ms");
    }

    @Ignore
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
            System.out.println("traversal " + Double.toString(duration / 100.0) + "ms");
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

    @Ignore
    public void testSPIRemote() throws Exception {
        tearDown(true);
        HippoRepositoryServer backgroundServer = null;
        HippoRepository server = null;
        Session session = null;
        try {
            backgroundServer = new HippoRepositoryServer("rmi://localhost:1099/hipporepository");
            backgroundServer.run("rmi://localhost:1099/hipporepository", true);
            Thread.sleep(3000);
            server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository/spi");
            session = backgroundServer.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            build(session, content);
            session.save();
            session.logout();
            session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            long duration = test(session, 100);
            System.out.println("traversal " + Double.toString(duration / 100.0) + "ms");
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

    @Ignore
    public void testSPIremoting() throws Exception {
        tearDown(true);
        HippoRepositoryServer backgroundServer = null;
        HippoRepository server = null;
        Session session = null;
        try {
            backgroundServer = new HippoRepositoryServer("rmi://localhost:1099/hipporepository");
            backgroundServer.run("rmi://localhost:1099/hipporepository", true);
            Thread.sleep(3000);
            server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository/spi");
            session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            Node node = session.getRootNode().getNode("hippo:configuration");
            Node canonical = ((HippoNode)node).getCanonicalNode();
            assertNotNull(canonical);
            assertTrue(canonical.isSame(node));
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
        for (int i = 0; i <= count; i++) {
            if (i == 1)
                tBefore = System.currentTimeMillis();
            Node node = root;
            StringTokenizer st = new StringTokenizer("test/noot/zus/jet/teun/vuur/weide/hok/duif", "/");
            while (st.hasMoreTokens()) {
                node = node.getNode(st.nextToken());
                for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                    Property property = iter.nextProperty();
                    property.getString();
                }
            }
        }
        tAfter = System.currentTimeMillis();
        return tAfter - tBefore;
    }
}
