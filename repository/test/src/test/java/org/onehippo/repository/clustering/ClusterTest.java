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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

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
public abstract class ClusterTest {

    private final static Logger log = LoggerFactory.getLogger(ClusterTest.class);

    private static final Boolean cleanup = Boolean.getBoolean(ClusterTest.class.getName() + ".cleanup");
    private static final String dbtype = System.getProperty(ClusterTest.class.getName() + ".dbtype", "h2");
    private static final String dbport = System.getProperty(ClusterTest.class.getName() + ".dbport", "9001");
    private static final String repo1Config = "/org/onehippo/repository/clustering/node1-repository-" + dbtype + ".xml";
    private static final String repo2Config = "/org/onehippo/repository/clustering/node2-repository-" + dbtype + ".xml";

    protected static final Credentials CREDENTIALS = new SimpleCredentials("admin", "admin".toCharArray());

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
        final File tmpdir = new File(System.getProperty("java.io.tmpdir"), ClusterTest.class.getSimpleName());
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

    private static void closeRepository(final Object repo) {
        try {
            repo.getClass().getMethod("close").invoke(repo);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to close repository: " + e);
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

    /**
     * Create a LocalHippoRepository running in its own isolated classloader. Because of this classloader
     * isolation all access to the repository internals must be done using reflection.
     */
    private static Object createRepository(final String repoPath, final String repoConfig) throws Exception {
        URLClassLoader contextClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        URLClassLoader classLoader = new RepositoryClassLoader(contextClassLoader.getURLs(), contextClassLoader);
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return Class.forName("org.hippoecm.repository.LocalHippoRepository", true, classLoader).
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

    /**
     * Check the index consistency of a repository. All access to the repository must be invoked using
     * reflection because it runs in an isolated classloader.
     */
    protected boolean checkIndexConsistency(Object repo) throws RepositoryException, IOException {
        final Object searchIndex = getSearchIndex(repo);
        final List errors;
        try {
            final Object consistencyCheck = searchIndex.getClass().getMethod("runConsistencyCheck").invoke(searchIndex);
            errors = (List) consistencyCheck.getClass().getMethod("getErrors").invoke(consistencyCheck);
            return errors.isEmpty();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to run index consistency check: " + e);
        }
        return false;
    }

    /**
     * Check the database consistency of a repository. All access to the repository must be invoked using
     * reflection because it runs in an isolated classloader.
     */
    protected boolean checkDatabaseConsistency(Object repo) throws RepositoryException {
        Object persistenceManager = getPersistenceManager(repo);
        ClassLoader classLoader = repo.getClass().getClassLoader();
        try {
            final Class<?> checkerClass = Class.forName("org.apache.jackrabbit.core.persistence.bundle.ConsistencyCheckerImpl", true, classLoader);
            final Class<?> pmClass = Class.forName("org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager", true, classLoader);
            final Class<?> listenerClass = Class.forName("org.apache.jackrabbit.core.persistence.check.ConsistencyCheckListener", true, classLoader);
            final Class<?> channelClass = Class.forName("org.apache.jackrabbit.core.cluster.UpdateEventChannel", true, classLoader);
            final Constructor<?> checkerConstructor = checkerClass.getConstructor(pmClass, listenerClass, String.class, channelClass);
            final Object checker = checkerConstructor.newInstance(persistenceManager, null, null, null);
            checkerClass.getMethod("check", String[].class, boolean.class).invoke(checker, new String[] { session1.getNode("/test").getIdentifier() }, true);
            // todo: check() with a determinate list of ids seems to be broken, we need to run a double check because of that
            checkerClass.getMethod("doubleCheckErrors").invoke(checker);
            final Object report = checkerClass.getMethod("getReport").invoke(checker);
            Set errors = (Set) report.getClass().getMethod("getItems").invoke(report);
            return errors.isEmpty();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to run database consistency check: " + e);
        }
        return false;
    }

    private Object getPersistenceManager(Object repo) throws RepositoryException {
        final Repository repository = getRepository(repo);
        try {
            return repository.getClass().getMethod("getPersistenceManager", String.class).invoke(repository, "default");
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to get PersistenceManager from RepositoryImpl: " + e);
        }
        return null;
    }

    private Object getSearchIndex(Object repo) throws RepositoryException {
        final Repository repository = getRepository(repo);
        try {
            return repository.getClass().getMethod("getHippoQueryHandler", String.class).invoke(repository, "default");
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to get QueryHandler from RepositoryImpl:" + e);
        }
        return null;
    }

    private String getTestSystemViewXML(Session session) throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        session.exportSystemView("/test", out, false, false);
        return out.toString();
    }

    private Repository getRepository(Object repo) {
        try {
            Object decorator = repo.getClass().getMethod("getRepository").invoke(repo);
            final Field repository = decorator.getClass().getDeclaredField("repository");
            repository.setAccessible(true);
            return (Repository) repository.get(decorator);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
            log.error("Failed to get internal RepositoryImpl from LocalHippoRepository: " + e);
        }
        return null;
    }

    protected Session loginSession(Object repo) {
        try {
            return (Session) repo.getClass().getMethod("login", Credentials.class).invoke(repo, CREDENTIALS);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to log in session: " + e);
        }
        return null;
    }

    /**
     * ClassLoader to run each repository in isolation to each other. Only the JCR API
     * is shared with the test class so that test cases can use JCR without reflection.
     */
    private static class RepositoryClassLoader extends URLClassLoader {

        private final URLClassLoader shared;

        public RepositoryClassLoader(final URL[] urls, final URLClassLoader shared) {
            super(urls, null);
            this.shared = shared;
        }

        @Override
        public Class<?> loadClass(final String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("javax.jcr")) {
                return shared.loadClass(name);
            }
            return super.loadClass(name, resolve);
        }
    }
}
