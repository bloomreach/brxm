/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.onehippo.cm.engine.ConfigurationServiceTestUtils.createChildNodesString;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_ORDER_BEFORE;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_ORDER_BEFORE_FIRST;
import static org.onehippo.cm.model.Constants.META_ORDER_BEFORE_FIRST;

public class AutoExportIntegrationTest {

    /* In their simplest form, these tests work as follows: the Fixture class loads a named fixture from
     * resource/AutoExportIntegrationTest. This named fixture contains two folders: 'in' and 'out'. The 'in' folder is
     * copied to a temporary folder, which is then used as the project folder by AutoExport. A repository is started
     * and after that, a new session is created, which is handed to the lambda in the test method. When that lambda
     * returns, the session is saved and the Fixture waits for AutoExport to export the changes. Once that is done, the
     * temporary folder is compared to the 'out' resource folder.
     *
     * The fixture class also has some additional capabilities:
     * - perform pre and post validation, see #autoexport_reorder_content_only
     * - initialize AutoExport using multiple modules, see #autoexport_reorder_second_module for an example
     * - perform multiple runs, see #reapply_content, the first run starts the repository with an empty database,
     *   stops the repository again, restarts the repository reusing the existing database
     */

    @Test
    public void content_filters() throws Exception {
        // test that autoexport:filteruuids and autoexport:excluded are applied to content
        new Fixture("content_filters").test(session -> {
            session.getNode("/content/uuids").setProperty("newProperty", "value");
            session.getNode("/content/uuids/child").setProperty("newProperty", "value");
            session.getNode("/content")
                    .addNode("excluded", "nt:unstructured")
                    .addNode("child", "nt:unstructured");
        });
    }

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

    @Test
    public void autoexport_reorder_within_module() throws Exception {
        new Fixture("autoexport_reorder_within_module").test(
            (session, configurationModel) -> {
                assertOrder("[a1-initial, a2-last, a3-first, a4-middle]", "/across", session, configurationModel);

                assertOrder("[w1-initial, w2-last, w3-first, w4-middle]", "/within/sub", session, configurationModel);

                assertOrderInJcr("[m1-initial, m2-last, m3-content1, m4-content2]", "/mix", session);
                assertOrderModel("[m1-initial, m2-last]", "/mix", configurationModel);

                assertOrder("[1-first, 2-second, 3-third, delete]", "/reorder-on-config-delete", session, configurationModel);

                assertOrderInJcr("[n3, n2, n1]", "/reorder-on-content-delete", session);
                assertOrderModel("[]", "/reorder-on-content-delete", configurationModel);

                assertOrder("[a, c, b]", "/existing-files/sub", session, configurationModel);
            },
            (session) -> {
                final Node across = session.getNode("/across");
                across.orderBefore("a3-first", "a1-initial");
                across.orderBefore("a4-middle", "a2-last");

                final Node within = session.getNode("/within/sub");
                within.orderBefore("w3-first", "w1-initial");
                within.orderBefore("w4-middle", "w2-last");

                final Node mix = session.getNode("/mix");
                mix.orderBefore("m3-content1", "m2-last");
                mix.orderBefore("m4-content2", "m3-content1");

                // Note that this test also demonstrates cleaning the no-longer-necessary order-before of '2-second',
                // '3-third' and the entire (superfluous) source reorder-on-config-delete-1-first.yaml
                session.getNode("/reorder-on-config-delete/delete").remove();

                session.getNode("/reorder-on-content-delete/n2").remove();

                session.getNode("/existing-files/sub").orderBefore("b", "c");

                final Node createNewFiles = session.getNode("/create-new-files/sub");
                createNewFiles.addNode("zzz-first", "nt:unstructured");
                createNewFiles.addNode("abc-last", "nt:unstructured");
            },
            (session, configurationModel) -> {
                assertOrder("[a3-first, a1-initial, a4-middle, a2-last]", "/across", session, configurationModel);

                assertOrder("[w3-first, w1-initial, w4-middle, w2-last]", "/within/sub", session, configurationModel);

                assertOrderInJcr("[m1-initial, m4-content2, m3-content1, m2-last]", "/mix", session);
                assertOrderModel("[m1-initial, m2-last]", "/mix", configurationModel);

                assertOrder("[1-first, 2-second, 3-third]", "/reorder-on-config-delete", session, configurationModel);

                assertOrderInJcr("[n3, n1]", "/reorder-on-content-delete", session);
                assertOrderModel("[]", "/reorder-on-content-delete", configurationModel);

                assertOrder("[a, b, c]", "/existing-files/sub", session, configurationModel);

                assertOrder("[zzz-first, abc-last]", "/create-new-files/sub", session, configurationModel);
            });
    }

    @Test
    public void autoexport_reorder_content_only() throws Exception {
        new Fixture("reorder_content_only").test(
            (session, configurationModel) -> {
                assertOrderInJcr("[c1, c2]", "/content", session);
            },
            (session) -> {
                final Node content = session.getNode("/content");
                content.orderBefore("c2", "c1");
            },
            (session, configurationModel) -> {
                assertOrderInJcr("[c2, c1]", "/content", session);
            });
    }

    @Test
    public void autoexport_reorder_sns() throws Exception {
        new Fixture("reorder_sns").test(
            (session, configurationModel) -> {
                // intentionally empty
            },
            (session) -> {
                final Node configWithSns = session.getNode("/config-with-sns");
                final Node snsInSameSource = configWithSns.addNode("sns", "nt:unstructured");
                snsInSameSource.setProperty("property", "value2");
                configWithSns.orderBefore("sns[2]", "sns[1]");

                final Node subWithSns = session.getNode("/config-with-sub/sub-with-sns");
                final Node snsInNewSource = subWithSns.addNode("sns", "nt:unstructured");
                snsInNewSource.setProperty("property", "value3");
                subWithSns.orderBefore("sns[3]", "sns[1]");
            },
            (session, configurationModel) -> {
                assertOrderInJcr("[sns[1], sns[2]]", "/config-with-sns", session);
                assertOrderModel("[sns, sns[2]]", "/config-with-sns", configurationModel);
                assertEquals("value2", JcrUtils.getStringProperty(session.getNode("/config-with-sns/sns[1]"), "property", ""));
                assertEquals("value1", JcrUtils.getStringProperty(session.getNode("/config-with-sns/sns[2]"), "property", ""));

                assertOrderInJcr("[sns[1], sns[2], sns[3]]", "/config-with-sub/sub-with-sns", session);
                assertOrderModel("[sns, sns[2], sns[3]]", "/config-with-sub/sub-with-sns", configurationModel);
                assertEquals("value3", JcrUtils.getStringProperty(session.getNode("/config-with-sub/sub-with-sns/sns[1]"), "property", ""));
                assertEquals("value1", JcrUtils.getStringProperty(session.getNode("/config-with-sub/sub-with-sns/sns[2]"), "property", ""));
                assertEquals("value2", JcrUtils.getStringProperty(session.getNode("/config-with-sub/sub-with-sns/sns[3]"), "property", ""));
            });
    }

    @Test
    public void autoexport_reorder_upstream_module() throws Exception {
        final String noLocalDefsPath = "/AutoExportIntegrationTest/reorder-upstream-module/no-local-defs";
        final String localDefsPath = "/AutoExportIntegrationTest/reorder-upstream-module/local-defs";
        new Fixture("reorder_upstream_module").test(
            (session, configurationModel) -> {
                assertOrder("[first, second, third]", noLocalDefsPath, session, configurationModel);
                assertOrder("[first-local, 1-first, 3-third, 2-second, 4-fourth, last-local]", localDefsPath, session, configurationModel);
            },
            (session) -> {
                final Node noLocalDefNode = session.getNode(noLocalDefsPath);
                noLocalDefNode.orderBefore("third", "first");
                noLocalDefNode.orderBefore("second", "first");

                final Node localDefNode = session.getNode(localDefsPath);
                localDefNode.orderBefore("4-fourth", "3-third");
                localDefNode.orderBefore("1-first", null);
            },
            (session, configurationModel) -> {
                assertOrder("[third, second, first]", noLocalDefsPath, session, configurationModel);
                assertOrder("[first-local, 4-fourth, 3-third, 2-second, last-local, 1-first]", localDefsPath, session, configurationModel);
            });
    }

    @Test
    public void autoexport_reorder_second_module() throws Exception {
        final String reorderWithin = "/reorder-second-within";
        final String reorderMix = "/reorder-second-mix";

        final ModuleInfo first = new ModuleInfo("reorder_second_module", "first");
        final ModuleInfo second = new ModuleInfo("reorder_second_module", "second");

        new Fixture(first, second).test(
                (session, configurationModel) -> {
                    assertOrder("[one, two]", reorderWithin, session, configurationModel);
                    assertOrder("[from-first, from-second]", reorderMix, session, configurationModel);
                },
                (session) -> {
                    final Node reorderWithinNode = session.getNode(reorderWithin);
                    reorderWithinNode.orderBefore("two", "one");

                    final Node reorderMixNode = session.getNode(reorderMix);
                    reorderMixNode.orderBefore("from-second", "from-first");
                },
                (session, configurationModel) -> {
                    assertOrder("[two, one]", reorderWithin, session, configurationModel);
                    assertOrder("[from-second, from-first]", reorderMix, session, configurationModel);
                });
    }

    @Test
    public void reapply_content() throws Exception {
        final ModuleInfo cycle1Module = new ModuleInfo("reapply_content", "cycle1", "in", "in");
        final ModuleInfo cycle2Module = new ModuleInfo("reapply_content", "cycle2", "in", "in");

        final String moduleBaselineRoot = "/hcm:hcm/hcm:baseline/hippo-cms-test/autoexport-integration-test-project/autoexport-integration-test-module";

        final Validator validateBaselineAfterCycle1 = (session, configurationModel) -> {
            final Node baselineNode1 = session.getNode(moduleBaselineRoot + "/hcm-content/node1.yaml");
            final Node baselineNode2 = session.getNode(moduleBaselineRoot + "/hcm-content/node2.yaml");
            assertFalse(baselineNode1.hasProperty(HCM_CONTENT_ORDER_BEFORE));
            assertEquals("node1", baselineNode2.getProperty(HCM_CONTENT_ORDER_BEFORE).getString());
            assertTrue(session.getNode(moduleBaselineRoot + "/hcm-content").hasNode("node3.yaml"));
            assertTrue(session.getNode(moduleBaselineRoot + "/hcm-content").hasNode("node4.yaml"));

            assertOrderInJcr("[node2, node3, node1, node4]", "/content-test", session);
            assertEquals("node1-cycle1", session.getProperty("/content-test/node1/property").getString());
            assertEquals("node2-cycle1", session.getProperty("/content-test/node2/property").getString());
            assertEquals("node3-cycle1", session.getProperty("/content-test/node3/property").getString());

            // 3 from module 'hippo-repository-engine-test' and 4 from this test
            assertEquals(7, configurationModel.getContentDefinitions().size());
            assertEquals(null, getOrderBefore(configurationModel, "/content-test/node1"));
            assertEquals("node1", getOrderBefore(configurationModel, "/content-test/node2"));
        };
        final Run run1 = new Run(cycle1Module, validateBaselineAfterCycle1, (session) -> {}, NOOP);

        final Validator validateBaselineAfterCycle2 = (session, configurationModel) -> {
            final Node baselineNode1 = session.getNode(moduleBaselineRoot + "/hcm-content/node1.yaml");
            final Node baselineNode2 = session.getNode(moduleBaselineRoot + "/hcm-content/node2.yaml");
            assertEquals(HCM_CONTENT_ORDER_BEFORE_FIRST, baselineNode1.getProperty(HCM_CONTENT_ORDER_BEFORE).getString());
            assertFalse(baselineNode2.hasProperty(HCM_CONTENT_ORDER_BEFORE));
            assertTrue(session.getNode(moduleBaselineRoot + "/hcm-content").hasNode("node3.yaml"));
            assertFalse(session.getNode(moduleBaselineRoot + "/hcm-content").hasNode("node4.yaml"));

            assertOrderInJcr("[node1, node3, node2]", "/content-test", session);
            assertEquals("node1-cycle2", session.getProperty("/content-test/node1/property").getString());
            assertEquals("node2-cycle2", session.getProperty("/content-test/node2/property").getString());
            assertEquals("node3-cycle1", session.getProperty("/content-test/node3/property").getString());

            // 3 from module 'hippo-repository-engine-test' and 3 from this test
            assertEquals(6, configurationModel.getContentDefinitions().size());
            assertEquals(META_ORDER_BEFORE_FIRST, getOrderBefore(configurationModel, "/content-test/node1"));
            assertEquals(null, getOrderBefore(configurationModel, "/content-test/node2"));
        };
        final Run run2 = new Run(cycle2Module, validateBaselineAfterCycle2, (session) -> {}, NOOP);

        new Fixture().run(run1, run2);
    }

    @Test
    public void reorder_within_upstream_content() throws Exception {
        // In the node 'addNewLocal', three upstream nodes are bootstrapped.
        // The upstream node 'up-c' is deleted by a local action.
        // When adding a new node, it is expected to be exported as a new local content definition.
        final String addNewLocal = "/AutoExportIntegrationTest-reorder-within-upstream-content";

        // In the node 'overlapping', two upstream nodes are bootstrapped, and two local definitions.
        // The two local definitions are ordered before the two upstream nodes.
        // When 'overlapping' is reordered (triggered by a delete of one the local definitions), it is not possible to
        // correctly order the nodes in overlapping, for which a warning is written in the log. As a result, the order
        // before of the remaining local definition disappears, while in jcr it remains ordered before the upstream.
        final String overlapping = "/AutoExportIntegrationTest-reorder-within-upstream-content/up-b/overlapping";

        new Fixture("reorder_within_upstream_content").test(
                (session, configurationModel) -> {
                    assertOrderInJcr("[up-b, up-a]", addNewLocal, session);
                    assertOrderInJcr("[local-b, local-a, up-a, up-b]", overlapping, session);
                },
                (session) -> {
                    final Node content = session.getNode(addNewLocal);
                    content.addNode("local", "nt:unstructured");
                    content.orderBefore("local", "up-b");

                    session.getNode(overlapping).getNode("local-b").remove();
                },
                (session, configurationModel) -> {
                    assertOrderInJcr("[local, up-b, up-a]", addNewLocal, session);
                    assertOrderInJcr("[local-a, up-a, up-b]", overlapping, session);
                });
    }

    @Test @Ignore
    public void change_within_downstream_overlapping_content() throws Exception {
        final String overlapping = "/AutoExportIntegrationTest-reorder-within-upstream-content/up-b/overlapping";

        new Fixture("change_within_downstream_overlapping_content").test(
                NOOP,
                (session) -> {
                    session.getNode(overlapping).getNode("local-b").setProperty("newProperty", "value");
                },
                NOOP);
    }

    private void assertOrder(final String order, final String path, final Session session,
                             final ConfigurationModelImpl model) throws Exception {
        assertOrderInJcr(order, path, session);
        assertOrderModel(order, path, model);
    }

    private void assertOrderInJcr(final String order, final String path, final Session session) throws Exception {
        assertEquals(order, createChildNodesString(session.getNode(path)));
    }

    private void assertOrderModel(final String order, final String path, final ConfigurationModelImpl model) throws Exception {
        assertEquals(order, model.resolveNode(path).getNodeNames().toString().replaceAll("\\[1]", ""));
    }

    private String getOrderBefore(final ConfigurationModelImpl configurationModel, final String definitionRootPath) {
        return configurationModel.getContentDefinitions().stream()
                .filter(d -> d.getRootPath().equals(definitionRootPath))
                .findFirst().get().getNode().getOrderBefore();
    }

    // this test can only be run if a failure tripwire is enabled in DefinitionMergeService.mergeConfigDefinitionNode()
    // by un-commenting lines at the beginning of that method
    @Test @Ignore
    public void merge_error_handling() throws Exception {
        new Fixture("merge_error_handling").test(
            NOOP,
            session -> {
                session.getNode("/config").addNode("TestNodeThatShouldNotBeInTheModel");
                session.getNode("/config").addNode("TestNodeThatShouldCauseAnExceptionOnlyInTesting");
            },
            (session, configurationModel) -> {
                assertNull(configurationModel.resolveNode("/config").getNode("TestNodeThatShouldNotBeInTheModel"));
            }
        );
    }

    // this test can only be run if a failure tripwire is enabled in AutoExportModuleWriter.sourceShouldBeSkipped()
    // by un-commenting lines at the beginning of that method
    @Test @Ignore
    public void write_error_handling() throws Exception {
        new Fixture("write_error_handling").test(
            NOOP,
            session -> {
                session.getNode("/config").addNode("TestNodeThatShouldNotBeInTheModel");
            },
            (session, configurationModel) -> {
                assertNull(configurationModel.resolveNode("/config").getNode("TestNodeThatShouldNotBeInTheModel"));
            }
        );
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    // Validator that checks nothing -- used as a stand-in when you want only a pre- or only a post-validator
    private static final Validator NOOP = (session, configurationModel) -> {};

    private class Fixture extends AbstractBaseTest {

        private final ModuleInfo[] modules;
        private final Path projectPath;

        Fixture() throws IOException {
            this(new ModuleInfo[0]);
        }

        Fixture(final String fixtureName) throws IOException {
            this(new ModuleInfo(fixtureName));
        }

        Fixture(final ModuleInfo... modules) throws IOException {
            this.projectPath = folder.newFolder("project").toPath();
            this.modules = modules;
        }

        void test(final JcrRunner jcrRunner) throws Exception {
            test(NOOP, jcrRunner, NOOP);
        }

        void test(final Validator preConditionValidator,
                  final JcrRunner jcrRunner,
                  final Validator postConditionValidator) throws Exception {
            run(new Run(modules, preConditionValidator, jcrRunner, postConditionValidator));
        }

        void run(final Run... runs) throws Exception {
            for (final Run run : runs) {
                FileUtils.cleanDirectory(projectPath.toFile());

                final List<URL> additionalClasspathURLs = new ArrayList<>(run.getModules().length);
                for (final ModuleInfo module : run.getModules()) {
                    final Path workingDirectory =
                            projectPath.resolve(Paths.get(module.getEffectiveModuleName(), "src", "main", "resources"));
                    module.setWorkingDirectory(workingDirectory);
                    Files.createDirectories(workingDirectory);
                    FileUtils.copyDirectory(module.getInPath().toFile(), workingDirectory.toFile());
                    additionalClasspathURLs.add(workingDirectory.toAbsolutePath().toUri().toURL());
                }

                final IsolatedRepository repository =
                        new IsolatedRepository(folder.getRoot(), projectPath.toFile(), additionalClasspathURLs);

                repository.startRepository();
                final Session session = repository.login(IsolatedRepository.CREDENTIALS);

                // verify that auto-export is disabled, since we want to control when it runs
                assertTrue(session.nodeExists("/hippo:configuration/hippo:modules/autoexport/hippo:moduleconfig"));
                assertFalse(session.getNode("/hippo:configuration/hippo:modules/autoexport/hippo:moduleconfig")
                        .getProperty("autoexport:enabled").getBoolean());

                run.getPreConditionValidator().validate(session, repository.getRuntimeConfigurationModel());

                // Run AutoExport to set its lastRevision ...
                repository.runSingleAutoExportCycle();

                run.getJcrRunner().run(session);
                session.save();

                // ... and run it again to export the changes made by jcrRunner
                repository.runSingleAutoExportCycle();

                session.refresh(false);
                run.getPostConditionValidator().validate(session, repository.getRuntimeConfigurationModel());

                session.logout();
                repository.stop();

                for (final ModuleInfo module : run.getModules()) {
                    assertNoFileDiff(
                            "In fixture '" + module.getFixtureName() + "', the module '" + module.getEffectiveModuleName()
                                    + "' is not as expected",
                            module.getOutPath(),
                            module.getWorkingDirectory());
                }
            }
        }
    }

}
