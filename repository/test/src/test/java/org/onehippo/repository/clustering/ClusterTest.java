/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.clustering;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assume.assumeTrue;

/**
 * Starts up two repositories backed by the same database
 * allowing subclasses to perform cluster concurrency tests.
 * The Repositories are run in their own isolated classloaders
 * to prevent sharing of static caches. Only JCR API classes
 * are shared with the test classes.
 * <p>
 *     By default starts an h2 database server as a backend for
 *     the repositories on port 9001. MySQL can also be used by running with the option
 *     -Dorg.onehippo.repository.clustering.ClusterTest.dbtype=mysql
 *     An empty database called jcr must be present and accessible to
 *     user root with an empty password.
 *     You can also change the port on which h2 is started by using the option
 *     -Dorg.onehippo.repository.clustering.ClusterTest.dbport
 * </p>
 * <p>
 *     To control whether the repositories and database be cleaned out before
 *     and after running the test, use
 *     -Dorg.onehippo.repository.clustering.ClusterTest.cleanup=true
 *     Default is false.
 * </p>
 */
public abstract class ClusterTest extends ClusterUtilitiesTest {

    private final static Logger log = LoggerFactory.getLogger(ClusterTest.class);

    private static final Boolean cleanup = true;
    private static final String dbtype = System.getProperty(ClusterTest.class.getName() + ".dbtype", "h2");
    private static final String dbport = System.getProperty(ClusterTest.class.getName() + ".dbport", "9001");
    private static final String repo1Config = "/org/onehippo/repository/clustering/node1-repository-" + dbtype + ".xml";
    private static final String repo2Config = "/org/onehippo/repository/clustering/node2-repository-" + dbtype + ".xml";

    private static File tmpdir;
    private static String repo1Path;
    private static String repo2Path;

    protected static Object repo1;
    protected static Object repo2;

    private static String h2Path;
    private static Server server;

    protected Session session1;
    protected Session session2;

    @BeforeClass
    public static void startRepositories() throws Exception {
        startRepositories(cleanup);
    }

    protected static void startRepositories(boolean cleanup) throws Exception {
        tmpdir = Files.createTempDirectory(ClusterTest.class.getSimpleName()).toFile();
        final File repo1Dir = new File(tmpdir, "repository-node1");
        if (!repo1Dir.exists()) {
            repo1Dir.mkdir();
        }
        repo1Path = repo1Dir.getAbsolutePath();
        final File repo2Dir = new File(tmpdir, "repository-node2");
        if (!repo2Dir.exists()) {
            repo2Dir.mkdir();
        }
        repo2Path = repo2Dir.getAbsolutePath();
        final File h2Dir = new File(tmpdir, "h2");
        if (!h2Dir.exists()) {
            h2Dir.mkdir();
        }
        h2Path = h2Dir.getAbsolutePath();
        if (cleanup) {
            cleanup();
        }
        if (dbtype.equals("h2")) {
            server = Server.createTcpServer("-tcpPort", dbport, "-baseDir", h2Path).start();
        }
        final String repoPathSysProp = System.getProperty("repo.path", "");
        System.setProperty("repo.path", "");
        System.setProperty("rep.dbport", dbport);
        if (repo1 == null) {
            repo1 = createRepository(repo1Path, repo1Config);
        }
        if (repo2 == null) {
            repo2 = createRepository(repo2Path, repo2Config);
        }
        System.setProperty("repo.path", repoPathSysProp);
    }

    @AfterClass
    public static void stopRepositories() throws Exception {
        stopRepositories(cleanup);
    }

    protected static void stopRepositories(boolean cleanup) throws Exception {
        if (repo1 != null) {
            closeRepository(repo1);
        }
        if (repo2 != null) {
            closeRepository(repo2);
        }
        if (server != null) {
            server.stop();
        }
        if (cleanup) {
            cleanup();
        }
    }

    private static void cleanup() throws IOException {
        final File repo1Dir = new File(repo1Path);
        if (repo1Dir.exists()) {
            FileUtils.cleanDirectory(repo1Dir);
        }
        final File repo2Dir = new File(repo2Path);
        if (repo2Dir.exists()) {
            FileUtils.cleanDirectory(repo2Dir);
        }
        final File h2Dir = new File(h2Path);
        if (h2Dir.exists()) {
            FileUtils.cleanDirectory(h2Dir);
        }

        FileUtils.deleteDirectory(tmpdir);
    }

    @Before
    public void setUp() throws Exception {
        session1 = loginSession(repo1);
        session2 = loginSession(repo2);

        session1.getRootNode().addNode("test");
        session1.save();
        session2.refresh(false);
        assumeTrue(session2.nodeExists("/test"));
    }

    @After
    public void tearDown() throws Exception {
        if (session1 != null && session1.isLive()) {
            session1.refresh(false);
            removeNodes("/test");
            session1.logout();
        }
        if (session2 != null && session2.isLive()) {
            session2.logout();
        }
    }

    protected void removeNodes(final String path) throws RepositoryException {
        while (session1.nodeExists(path)) {
            session1.getNode(path).remove();
        }
        session1.save();
    }


    protected boolean clusterContentEqual() throws RepositoryException, IOException {
        session1.refresh(false);
        session2.refresh(false);
        final String systemViewXml1 = getTestSystemViewXML(session1);
        final String systemViewXml2 = getTestSystemViewXML(session2);

        if (systemViewXml1.equals(systemViewXml2)) {
            return true;
        } else {
            log.error("Cluster content is not equal:\ncluster1\n{}\ncluster2\n{}\n", systemViewXml1, systemViewXml2);
            return false;
        }
    }

}
