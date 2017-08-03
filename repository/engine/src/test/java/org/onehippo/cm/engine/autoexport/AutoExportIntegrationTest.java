/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.engine.autoexport;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cm.model.AbstractBaseTest;

public class AutoExportIntegrationTest {

    /* These tests work as follows: the Fixture class loads a named fixture from the resource/AutoExportIntegrationTest
     * folder. This named fixture contains two folders: "in" and "out". The "in" folder is copied to a temporary folder,
     * which is then used as the project folder by  AutoExport. A repository is started and after that, a new session is
     * created, which is handed to the lambda in the test method. When that lambda returns, the session is saved and the
     * Fixture waits for AutoExport to export the changes. Once that is done, the temporary folder is compared to the
     * "out" resource folder.
     */

    @Test
    public void config_sns_create_new() throws Exception {
        new Fixture("config_sns_create_new").test(session -> {
            final Node container = session.getNode("/config").addNode("container", "nt:unstructured");
            final Node sns1 = container.addNode("sns", "nt:unstructured");
            sns1.setProperty("new", "value1");
            final Node sns2 = container.addNode("sns", "nt:unstructured");
            sns2.setProperty("new", "value2");
        });
    }

    @Test
    public void config_sns_deeptree() throws Exception {
        new Fixture("config_sns_deeptree").test(session -> {
            final Node container = session.getNode("/config/deep/tree");
            container.getNode("sns[1]").setProperty("new", "value1");
            container.getNode("sns[3]").setProperty("new", "value3");
            container.getNode("sns[2]").remove();
        });
    }

    @Test
    public void config_sns_delete() throws Exception {
        new Fixture("config_sns_delete").test(session -> {
            final Node container = session.getNode("/config/container");
            container.getNode("sns[1]").setProperty("new", "value1");
            container.getNode("sns[3]").setProperty("new", "value3");
            container.getNode("sns[2]").remove();
        });
    }

    @Test
    public void config_sns_update_existing() throws Exception {
        new Fixture("config_sns_update_existing").test(session -> {
            final Node container = session.getNode("/config/container");
            container.getNode("sns").setProperty("new", "value1");
            final Node sns2 = container.addNode("sns", "nt:unstructured");
            sns2.setProperty("new", "value2");
        });
    }

    @Test
    public void content_sns_create_new() throws Exception {
        new Fixture("content_sns_create_new").test(session -> {
            final Node container = session.getNode("/content").addNode("container", "nt:unstructured");
            final Node sns1 = container.addNode("sns", "nt:unstructured");
            sns1.setProperty("new", "value1");
            final Node sns2 = container.addNode("sns", "nt:unstructured");
            sns2.setProperty("new", "value2");
        });
    }

    @Ignore
    @Test
    public void content_sns_delete() throws Exception {
        new Fixture("content_sns_delete").test(session -> {
            final Node container = session.getNode("/content/container");
            container.getNode("sns[1]").setProperty("new", "value1");
            container.getNode("sns[3]").setProperty("new", "value3");
            container.getNode("sns[2]").remove();
        });
    }

    @Test
    public void content_sns_update_existing() throws Exception {
        new Fixture("content_sns_update_existing").test(session -> {
            final Node container = session.getNode("/content/container");
            container.getNode("sns").setProperty("new", "value1");
            final Node sns2 = container.addNode("sns", "nt:unstructured");
            sns2.setProperty("new", "value2");
        });
    }

    @Test
    public void autoexport_config_inject_residual() throws Exception {
        new Fixture("autoexport_config_inject_residual").test(session -> {
            // this fixture uses the following configuration:
            // autoexport:injectresidualchildnodecategory: ['**/hst:workspace/**[hst:containercomponent]: content', '/base/*: system']
            final Node containers = session.getNode("/hst:hst/hst:configurations/hippogogreen/hst:workspace/hst:containers");

            // add a new container matching the pattern, its nodes should go into content, the property to config
            final Node container = containers.addNode("container", "hst:containercomponent");
            container.setProperty("container-property", "container-value");
            final Node containerItem = container.addNode("containeritem", "hst:containeritemcomponent");
            containerItem.setProperty("item-property", "item-value");

            // add another container not matching the pattern
            final Node nonMatch = containers.addNode("non-match", "nt:unstructured");
            nonMatch.addNode("containeritem", "hst:containeritemcomponent").setProperty("item-property", "item-value");

            // modify an existing container matching the pattern, it should stay in config
            final Node existing = containers.getNode("existing");
            existing.addNode("containeritem", "hst:containeritemcomponent").setProperty("item-property", "item-value");

            // add system node, only 'node' should go to config
            final Node node = session.getNode("/base").addNode("node", "nt:unstructured");
            node.addNode("ignored", "nt:unstructured");
            node.addNode("ignored", "nt:unstructured");
        });
    }

    @Test
    public void autoexport_config_override_residual() throws Exception {
        new Fixture("autoexport_config_override_residual").test(session -> {
            // this fixture uses the following configuration:
            // autoexport:injectresidualchildnodecategory: ['**/hst:workspace/**[hst:containercomponent]: content']
            // autoexport:overrideresidualchildnodecategory: ['/hst:hst/hst:configurations: config', '**/children-ignored-by-config: system']
            final Node configurations = session.getNode("/hst:hst/hst:configurations");

            // add a new channel, the channel should be exported to config, the nodes within "container" to content
            // due to the injectresidual setting
            final Node mychannel = configurations.addNode("mychannel", "nt:unstructured");
            final Node container = mychannel.addNode("hst:workspace", "nt:unstructured")
                    .addNode("hst:containers", "nt:unstructured")
                    .addNode("container", "hst:containercomponent");
            container.setProperty("container-property", "container-value");
            final Node containerItem = container.addNode("containeritem", "hst:containeritemcomponent");
            containerItem.setProperty("item-property", "item-value");

            // the node "ignored" has explicit .meta:category: runtime
            // test that explicitly defined .meta:category is respected
            final Node ignored = configurations.addNode("ignored", "nt:unstructured");
            ignored.setProperty("ignored-property", "ignored-value");

            // new nodes under 'children-ignored-by-config' are expected to be ignored, properties go to config
            final Node ignoredByConfig = session.getNode("/test/children-ignored-by-config");
            ignoredByConfig.addNode("node", "nt:unstructured");
            ignoredByConfig.setProperty("property", "value");

            // new nodes under '/test' are expected to have category injected (content), and the subnode
            // 'inject-and-override' also has an explicit category override (config)
            final Node test = session.getNode("/test");
            test.addNode("inject-and-override", "nt:unstructured").addNode("config", "nt:unstructured");
            test.addNode("inject-only", "nt:unstructured").addNode("content", "nt:unstructured");
        });
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private class Fixture extends AbstractBaseTest {
        private final String name;
        private final boolean debug = false;
        Fixture(final String name) {
            this.name = name;
        }
        void test(final JcrRunner jcrRunner) throws Exception {
            final Path projectPath = folder.newFolder("project").toPath();
            final Path resourcePath = projectPath.resolve(Paths.get("TestModuleFileSource", "src", "main", "resources"));
            Files.createDirectories(resourcePath);

            final Path fixturePath = calculateBasePath().resolve(Paths.get("src", "test", "resources", "AutoExportIntegrationTest", name));
            FileUtils.copyDirectory(fixturePath.resolve("in").toFile(), resourcePath.toFile());

            final IsolatedRepository repository = new IsolatedRepository(folder.getRoot(), projectPath.toFile());

            repository.startRepository();
            final Session session = repository.login(IsolatedRepository.CREDENTIALS);

            try (final LogLineWaiterWrapper waiter = new LogLineWaiterWrapper(repository.getRepositoryClassLoader(),
                    EventJournalProcessor.class.getName(), Level.INFO)) {
                jcrRunner.run(session);
                session.save();

                final String autoExportDiff = "Computed diff";
                final String autoExportComplete = "Full auto-export cycle in";
                waiter.waitFor(new String[]{autoExportDiff, autoExportComplete},
                        debug ? LogLineWaiter.INFINITE_TIMEOUT : LogLineWaiter.DEFAULT_TIMEOUT);

                if (debug) {
                    final StringBuilder builder = new StringBuilder();
                    waiter.serializeAllEvents(builder);
                    System.out.println(builder.toString());
                }
            }

            session.logout();
            repository.stop();

            assertNoFileDiff("test " + name + " failed", fixturePath.resolve("out"), resourcePath);
        }
    }

    @FunctionalInterface
    private interface JcrRunner {
        void run(final Session session) throws Exception;
    }

    /**
     * Wrapper around {@link LogLineWaiter} that loads the {@link LogLineWaiter} object from a different classloader.
     * For the LogLineWaiter to capture any output, it needs to be loaded from the same classloader as the class that
     * is generating the log events.
     */
    private class LogLineWaiterWrapper implements AutoCloseable {
        private final Object collector;
        LogLineWaiterWrapper(final ClassLoader classLoader, final String loggerName, final Level level) throws Exception {
            final URLClassLoader contextClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                final Object levelObject = Class.forName(Level.class.getName(), true, classLoader)
                        .getMethod("forName", String.class, int.class)
                        .invoke(null, level.name(), level.intLevel());
                collector = Class.forName(LogLineWaiter.class.getName(), true, classLoader)
                        .getConstructor(String.class, levelObject.getClass())
                        .newInstance(loggerName, levelObject);
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
        void waitFor(final String[] messages, final int timeout) throws Exception {
            collector.getClass().getMethod("waitFor", String[].class, int.class).invoke(collector, messages, timeout);
        }
        public String serializeAllEvents(final StringBuilder builder) throws Exception {
            return (String) collector.getClass().getMethod("serializeAllEvents", StringBuilder.class).invoke(collector, builder);
        }
        @Override
        public void close() throws Exception {
            collector.getClass().getMethod("close").invoke(collector);
        }
    }

    /**
     * Utility method to calculate correct path in case when run under Intellij IDEA (Working directory should be set to
     * module's root, e.g. ../master/engine).
     * @return base directory
     */
    private static Path calculateBasePath() {
        String basedir = System.getProperty("basedir");
        basedir = basedir != null ? basedir: System.getProperty("user.dir");
        return Paths.get(basedir);
    }

}
