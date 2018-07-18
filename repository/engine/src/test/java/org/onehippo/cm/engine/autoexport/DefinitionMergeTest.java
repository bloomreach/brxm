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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.onehippo.cm.model.serializer.ModuleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptySet;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.onehippo.cm.model.util.FilePathUtils.nativePath;

public class DefinitionMergeTest {

    private static final Logger log = LoggerFactory.getLogger(DefinitionMergeTest.class);

    @Rule
    public TemporaryFolder output = new TemporaryFolder();

    @Test
    public void template_works() throws Exception {
        new MergeFixture("template").test();
    }

    @Test
    public void create_and_update_namespaces() throws Exception {
        new MergeFixture("namespace").test();
    }

    @Test
    public void new_node() throws Exception {
        new MergeFixture("new-node").test();
    }

    @Test
    public void new_node_empty_dev() throws Exception {
        new MergeFixture("new-node-empty-dev", "/", "").test();
    }

    @Test
    public void delete_node() throws Exception {
        new MergeFixture("delete-node").test();
    }

    @Test
    public void add_property() throws Exception {
        new MergeFixture("add-property", "/topmost,/otherTopmost,/exportFirstExistingRoot,/hippo:namespaces", "/").test();
    }

    @Test
    public void append_property() throws Exception {
        new MergeFixture("append-property").test();
    }

    @Test
    public void delete_property() throws Exception {
        new MergeFixture("delete-property").test();
    }

    @Test
    public void override_property() throws Exception {
        new MergeFixture("override-property").test();
    }

    @Test
    public void node_restore() throws Exception {
        new MergeFixture("node-restore").test();
    }

    @Test
    public void node_restore_identical() throws Exception {
        new MergeFixture("node-restore-identical").test();
    }

    @Test
    public void property_upstream_identical() throws Exception {
        new MergeFixture("property-upstream-identical", "/", "",
                true, new String[]{"exportFirst"}).test();
    }

    @Test
    public void property_upstream_partial() throws Exception {
        new MergeFixture("property-upstream-partial").test();
    }

    @Test
    public void property_upstream_multi() throws Exception {
        new MergeFixture("property-upstream-multi", "/", "",
                true, new String[]{"exportFirst"}).test();
    }

    @Test
    public void restore_property_identical() throws Exception {
        new MergeFixture("restore-property-identical").test();
    }

    @Test
    public void restore_property() throws Exception {
        new MergeFixture("restore-property").test();
    }

    public class MergeFixture extends AbstractBaseTest {
        String testName;
        String[] base = {"topmost", "upstream"};
        String[] toExport = {"exportFirst", "exportSecond"};
        AutoExportConfig autoExportConfig;

        public MergeFixture(final String testName) {
            this(testName, "/topmost,/exportFirstExistingRoot,/hippo:namespaces", "/");
        }

        public MergeFixture(final String testName, final String firstModulePaths, final String secondModulePaths) {
            this(testName, firstModulePaths, secondModulePaths, false, new String[] {"exportFirst", "exportSecond"});
        }

        public MergeFixture(final String testName, final String firstModulePaths, final String secondModulePaths,
                            final boolean singleModule, String[] toExport) {
            this.testName = testName;
            this.toExport = toExport;
            Map<String, Collection<String>> modules = new HashMap<>();
            modules.put("exportFirst", parsePathString(firstModulePaths));
            if (!singleModule) {
                modules.put("exportSecond", parsePathString(secondModulePaths));
            }
            autoExportConfig = new AutoExportConfig(true, modules, null, null);
        }


        private List<String> parsePathString(final String string) {
            if (string == null || string.equals("")) {
                return Collections.emptyList();
            }
            return Arrays.asList(string.split(","));
        }

        public MergeFixture base(final String... base) {
            this.base = base;
            return this;
        }

        public MergeFixture toExport(final String... toExport) {
            this.toExport = toExport;
            return this;
        }

        public MergeFixture config(final AutoExportConfig config) {
            this.autoExportConfig = config;
            return this;
        }

        public void test() throws Exception {
            try {
                log.debug("\n\nRunning test: {}", testName);

                final ConfigurationModelImpl model = new ConfigurationModelImpl();

                // build base
                for (String baseName : base) {
                    model.addReplacementModule(loadModule(in(testName, baseName)));
                }

                // build auto-export targets
                for (String toExportName : toExport) {
                    final ModuleImpl toExportModule = loadModule(in(testName, toExportName));
                    toExportModule.setMvnPath(toExportName);
                    model.addReplacementModule(toExportModule);
                }
                model.build();

                // load diff module
                final ModuleImpl diff = loadModule(in(testName, "diff"));

                final Session session = EasyMock.createNiceMock(Session.class);
                final Node node = EasyMock.createNiceMock(Node.class);
                final NodeType primaryNodeType = EasyMock.createNiceMock(NodeType.class);
                expect(session.getNode(anyObject())).andReturn(node).anyTimes();
                expect(node.getPrimaryNodeType()).andReturn(primaryNodeType).anyTimes();
                expect(primaryNodeType.hasOrderableChildNodes()).andReturn(false).anyTimes();
                replay(session, node, primaryNodeType);

                // merge diff
                DefinitionMergeService merger = new DefinitionMergeService(autoExportConfig, model, session);
                Collection<ModuleImpl> allMerged =
                        merger.mergeChangesToModules(diff, emptySet(), emptySet(), emptySet());

                for (ModuleImpl merged : allMerged) {
                    writeAndCompare(testName, merged);
                }
            }
            catch (Exception e) {
                e.printStackTrace(System.err);
                throw e;
            }
        }

        /**
         * Helper for loading a module for a specific test using the merge template pattern.
         */
        protected String in(String testName, String moduleName) {
            return "/merge/" + testName + "/in/" + moduleName;
        }

        protected Path in(String testName, ModuleImpl module) throws IOException {
            return findBase(in(testName, module.getName() + "/" + Constants.HCM_MODULE_YAML));
        }

        protected Path out(String testName, ModuleImpl module) throws IOException {
            return findBase(in(testName, module.getName() + "/" + Constants.HCM_MODULE_YAML))
                    .resolve(nativePath("../../out/"+module.getName()));
        }

        /**
         * Load a module given a root path in the resources for this test class.
         */
        protected ModuleImpl loadModule(String modulePath) throws Exception {
            return readFromResource(modulePath + "/" + Constants.HCM_MODULE_YAML).getModule();
        }

        protected void writeAndCompare(String testName, ModuleImpl module) throws Exception {
            final ModuleWriter writer = new ModuleWriter();

            // write module to a new temp dir
            Path mOut = output.newFolder(module.getName()).toPath();
            final ModuleContext moduleContext = new ModuleContext(module, in(testName, module));
            moduleContext.createOutputProviders(mOut.resolve(Constants.HCM_MODULE_YAML));
            writer.writeModule(module, moduleContext);

            // in case we expect an empty output for a module, we need to create the empty dir to compare against,
            // since the normal mvn resource copy will not recreate empty dirs in target
            final Path expectedRoot = out(testName, module);
            if (!expectedRoot.toFile().exists()) {
                Files.createDirectories(expectedRoot.toAbsolutePath());
            }

            // compare output to expected
            assertNoFileDiff(testName+": "+module.getName(), expectedRoot, mOut);
        }
    }
}
