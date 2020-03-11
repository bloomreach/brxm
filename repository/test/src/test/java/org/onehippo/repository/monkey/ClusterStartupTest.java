/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.monkey;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.repository.clustering.ClusterUtilitiesTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *     Integration test for making sure that a second cluster node starts up without index inconsistencies while the first
 *     repository is heavily changing nodes. The reason that this is an important scenario is that it touches very
 *     important jackrabbit code. The scenario that this integration test covers is the following
 *     <ul>
 *         <li>
 *              Cluster node 1 is running and making many fast changes
 *         </li>
 *         <li>
 *              During the changes being made by cluster node 1, cluster node 2 starts up
 *         </li>
 *         <li>
 *              Since cluster node 2 does not have an index, it will create one by traversing all jcr nodes. At the
 *              beginning, the revision id of cluster node 2 gets set to the global revision number
 *         </li>
 *         <li>
 *              When the indexing as a result of crawling has been done, cluster node 2 has to process all changes
 *              made by cluster 1.
 *         </li>
 *         <li>
 *              However, during crawling, cluster node 1 can have added a jcr node which has *already* been indexed, but
 *              is present in the journal table with a revision id larger than the revision id of cluster node 2. That
 *              means, that cluster node 2 will index that node again when processing the journal table changes. To
 *              avoid this, added nodes added *after* the revision id of cluster node 2 will *first* be removed again,
 *              and then when the revision table is processed, added again: as a result, duplicate entries in the index
 *              should be avoided
 *         </li>
 *     </ul>
 *     The second cluster node is once a cluster node that has consistency check at startup and once a cluster node (repo2)
 *     that has only repair duplicate entries (repo3)
 * </p>
 */
public class ClusterStartupTest extends ClusterUtilitiesTest {


    private static final Logger log = LoggerFactory.getLogger(ClusterStartupTest.class);

    private static final Boolean cleanup = true;
    private static final String dbtype = System.getProperty(ClusterStartupTest.class.getName() + ".dbtype", "h2");
    private static final String dbport = System.getProperty(ClusterStartupTest.class.getName() + ".dbport", "9001");
    private static final String repo1Config = "/org/onehippo/repository/clustering/node1-repository-" + dbtype + ".xml";
    private static final String repo2Config = "/org/onehippo/repository/clustering/node2-repository-" + dbtype + ".xml";
    private static final String repo3Config = "/org/onehippo/repository/clustering/node3-repository-" + dbtype + ".xml";

    private static File tmpdir;
    private static String repo1Path;
    private static String repo2Path;
    private static String repo3Path;

    protected static Object repo1;
    protected static Object repo2;
    protected static Object repo3;

    private static String h2Path;
    private static Server server;

    protected Session session1;

    private static int seed = Integer.getInteger(MonkeyTest.class.getName() + ".seed", -1);

    private Random random;
    private List<Action> actions;

    @BeforeClass
    public static void startRepositories() throws Exception {
        startRepositories(cleanup);
    }

    protected static void startRepositories(boolean cleanup) throws Exception {
        tmpdir = Files.createTempDirectory(ClusterStartupTest.class.getSimpleName()).toFile();
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

        final File repo3Dir = new File(tmpdir, "repository-node3");
        if (!repo3Dir.exists()) {
            repo3Dir.mkdir();
        }
        repo3Path = repo3Dir.getAbsolutePath();


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
        if (repo3 != null) {
            closeRepository(repo3);
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
        final File repo3Dir = new File(repo2Path);
        if (repo3Dir.exists()) {
            FileUtils.cleanDirectory(repo3Dir);
        }
        final File h2Dir = new File(h2Path);
        if (h2Dir.exists()) {
            FileUtils.cleanDirectory(h2Dir);
        }

        FileUtils.deleteDirectory(tmpdir);
    }

    final static AtomicInteger counter = new AtomicInteger(1);

    @Before
    public void setUp() throws Exception {
        session1 = loginSession(repo1);

        session1.getRootNode().addNode("test");
        session1.save();
        if (seed == -1) {
            seed = new Random().nextInt();
        }
        random = new Random(seed);
        log.info("Running MonkeyTest with seed={}", seed);

        actions = Arrays.asList(
                new AddNodeAction("a"), new AddNodeAction("b"), new AddNodeAction("a/b"),
                new AddNodeAction("a/c"), new AddNodeAction("a/d"),new AddNodeAction("a/e"),
                new AddNodeAction("a/f"), new AddNodeAction("a/g"),new AddNodeAction("a/h"),
                new AddNodeAction("a/i"), new AddNodeAction("a/j"),new AddNodeAction("a/k"),
                new AddNodeAction("b/c"), new AddNodeAction("b/d"),new AddNodeAction("b/e"),
                new AddNodeAction("b/f"), new AddNodeAction("b/g"),new AddNodeAction("b/h"),
                new AddNodeAction("b/i"), new AddNodeAction("b/j"),new AddNodeAction("b/k"),
                new RemoveNodeAction("a"), new RemoveNodeAction("b"), new RemoveNodeAction("a/b"),
                new MoveNodeAction("b", "a/b"), new MoveNodeAction("a/b", "b/a"), new MoveNodeAction("a", "b"),
                new SetPropertyAction("a/p"), new SetPropertyAction("a/b/p"),
                new RemovePropertyAction("a/p"), new RemovePropertyAction("a/b/p")
        );
    }

    @After
    public void tearDown() throws Exception {
        if (session1 != null && session1.isLive()) {
            session1.refresh(false);
            removeNodes("/test");
            session1.logout();
        }
    }

    protected void removeNodes(final String path) throws RepositoryException {
        while (session1.nodeExists(path)) {
            session1.getNode(path).remove();
        }
        session1.save();
    }

    @Test
    public void monkeyClusterStartUpTest_repairInconsistencies() throws Exception {
        Monkey monkey1 = createMonkey("monkey1", repo1);

        final AtomicBoolean run = new AtomicBoolean(true);
        asyncMonkeyTestThread(monkey1, run);
        // during the first repository making changes, start up a second repository
        if (repo2 == null) {
            repo2 = createRepository(repo2Path, repo2Config);
        }
        run.set(false);
        validate(repo2);
    }

    @Test
    public void monkeyClusterStartUpTest_repairDuplicateEntries() throws Exception {
        Monkey monkey1 = createMonkey("monkey1", repo1);

        final AtomicBoolean run = new AtomicBoolean(true);
        asyncMonkeyTestThread(monkey1, run);
        // during the first repository making changes, start up a second repository
        if (repo3 == null) {
            // repo3 has enableConsistencyCheck = false and as a result trigger duplicate repair only
            repo3 = createRepository(repo3Path, repo3Config);
        }
        run.set(false);
        validate(repo3);
    }

    private boolean validate(Object o) throws RepositoryException, IOException {
        log.info("checking cluster consistency...");
        return checkIndexConsistency(o);
    }

    private void asyncMonkeyTestThread(final Monkey monkey1, final AtomicBoolean run) {
        Thread thread = new Thread(() -> {
            while (run.get()) {
                try {
                    monkey1._do();
                    // save after every monkey action to trigger as many as possible journal events
                    monkey1.getSession().save();
                } catch (RepositoryException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private Monkey createMonkey(final String name, Object repo) throws RepositoryException {
        final Session session = loginSession(repo);
        return new Monkey(name, random, session, actions);
    }

}
