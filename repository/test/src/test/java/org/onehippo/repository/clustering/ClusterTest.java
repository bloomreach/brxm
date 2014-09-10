/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager;
import org.apache.jackrabbit.core.persistence.bundle.ConsistencyCheckerImpl;
import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;
import org.apache.jackrabbit.core.persistence.check.ReportItem;
import org.apache.jackrabbit.core.query.lucene.ConsistencyCheck;
import org.apache.jackrabbit.core.query.lucene.ConsistencyCheckError;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.h2.tools.Server;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.decorating.RepositoryDecorator;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assume.assumeTrue;

/**
 * Starts up two repositories backed by the same database
 * allowing subclasses to perform cluster concurrency tests.
 * <p>
 *     By default starts an h2 database server as a backend for
 *     the repositories. MySQL can also be used by running with the option
 *     -Dorg.onehippo.repository.clustering.ClusterTest.dbtype=mysql
 *     An empty database called jcr must be present and accessible to
 *     user root with an empty password.
 * </p>
 * <p>
 *     To control whether the repositories and database be cleaned out before
 *     and after running the test, use
 *     -Dorg.onehippo.repository.clustering.ClusterTest.cleanup=true
 *     Default is false.
 * </p>
 */
public abstract class ClusterTest {

    private final static Logger log = LoggerFactory.getLogger(ClusterTest.class);

    private static final Boolean cleanup = Boolean.getBoolean(ClusterTest.class.getName() + ".cleanup");
    private static final String dbtype = System.getProperty(ClusterTest.class.getName() + ".dbtype", "h2");
    private static final String repo1Config = "/org/onehippo/repository/clustering/node1-repository-" + dbtype + ".xml";
    private static final String repo2Config = "/org/onehippo/repository/clustering/node2-repository-" + dbtype + ".xml";

    protected static final Credentials CREDENTIALS = new SimpleCredentials("admin", "admin".toCharArray());

    private static String repo1Path;
    private static String repo2Path;

    protected static HippoRepository repo1;
    protected static HippoRepository repo2;

    private static String h2Path;
    private static Server server;

    protected Session session1;
    protected Session session2;

    @BeforeClass
    public static void setUpClass() throws Exception {
        final File tmpdir = new File(System.getProperty("java.io.tmpdir"));
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
        cleanup();
        if (dbtype.equals("h2")) {
            server = Server.createTcpServer("-tcpPort", "9001", "-baseDir", h2Path).start();
        }
        final String repoPathSysProp = System.getProperty("repo.path");
        System.setProperty("repo.path", "");
        if (repo1 == null) {
            repo1 = createRepository(repo1Path, repo1Config);
        }
        if (repo2 == null) {
            repo2 = createRepository(repo2Path, repo2Config);
        }
        System.setProperty("repo.path", repoPathSysProp);
    }

    @AfterClass
    public static void shutDownClass() throws Exception {
        if (repo1 != null) {
            repo1.close();
        }
        if (repo2 != null) {
            repo2.close();
        }
        if (server != null) {
            server.stop();
        }
        cleanup();
    }

    private static void cleanup() throws IOException {
        if (cleanup) {
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
        }
    }

    @Before
    public void setUp() throws Exception {
        session1 = repo1.login(CREDENTIALS);
        session2 = repo2.login(CREDENTIALS);

        session1.getRootNode().addNode("test");
        session1.save();
        session2.refresh(false);
        assumeTrue(session2.nodeExists("/test"));
    }

    public void tearDown() throws Exception {
        if (session1 != null && session1.isLive()) {
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

    private static HippoRepository createRepository(final String repoPath, final String repoConfig) throws Exception {
        URLClassLoader contextClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        URLClassLoader classLoader = new URLClassLoader(contextClassLoader.getURLs());
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return (HippoRepository) Class.forName("org.hippoecm.repository.LocalHippoRepository", true, classLoader).
                    getMethod("create", String.class, String.class).
                    invoke(null, repoPath, repoConfig);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    protected boolean clusterContentEqual() throws RepositoryException, IOException {
        session1.refresh(false);
        session2.refresh(false);
        return getTestSystemViewXML(session1).equals(getTestSystemViewXML(session2));
    }

    protected boolean checkIndexConsistency(HippoRepository repo) throws RepositoryException, IOException {
        final ConsistencyCheck consistencyCheck = getSearchIndex(repo).runConsistencyCheck();
        if (consistencyCheck.getErrors().size() > 0) {
            for (ConsistencyCheckError consistencyCheckError : consistencyCheck.getErrors()) {
                log.error("Index inconsistency: " + consistencyCheckError);
            }
            return false;
        }
        return true;
    }

    protected boolean checkDatabaseConsistency(HippoRepository repo, Session session) throws RepositoryException {
        ConsistencyCheckerImpl checker = new ConsistencyCheckerImpl(getPersistenceManager(repo), null, null, null);
        checker.check(new String[] { session.getNode("/test").getIdentifier() }, true);
        // todo: check() with a determinate list of ids seems to be broken, we need to run a double check because of that
        checker.doubleCheckErrors();
        final ConsistencyReport report = checker.getReport();
        if (!report.getItems().isEmpty()) {
            for (ReportItem reportItem : report.getItems()) {
                log.error("Database inconsistency: " + reportItem);
            }
            return false;
        }
        return true;
    }

    private AbstractBundlePersistenceManager getPersistenceManager(HippoRepository repo) throws RepositoryException {
        final RepositoryImpl repository = (RepositoryImpl) RepositoryDecorator.unwrap(repo.getRepository());
        return (AbstractBundlePersistenceManager) repository.getPersistenceManager("default");
    }

    private SearchIndex getSearchIndex(HippoRepository repo) throws RepositoryException {
        final RepositoryImpl repository = (RepositoryImpl) RepositoryDecorator.unwrap(repo.getRepository());
        return (SearchIndex) repository.getHippoQueryHandler("default");
    }

    private String getTestSystemViewXML(Session session) throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        session.exportSystemView("/test", out, false, false);
        return out.toString();
    }

}
