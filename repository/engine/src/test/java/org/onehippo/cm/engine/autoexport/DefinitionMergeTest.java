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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.FileConfigurationWriter;
import org.onehippo.cm.model.ModuleContext;
import org.onehippo.cm.model.PathConfigurationReader;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonList;
import static org.onehippo.cm.model.Constants.DEFAULT_EXPLICIT_SEQUENCING;

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
    public void delete_node() throws Exception {
        new MergeFixture("delete-node").test();
    }

    @Test
    public void add_property() throws Exception {
        new MergeFixture("add-property").test();
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


    public class MergeFixture extends AbstractBaseTest {
        String testName;
        String[] base = {"topmost", "upstream"};
        String[] toExport = {"exportFirst", "exportSecond"};
        Configuration autoExportConfig;

        public MergeFixture(final String testName) {
            this.testName = testName;

            Map<String, Collection<String>> modules = new HashMap<>();
            modules.put("exportFirst", Arrays.asList("/topmost", "/exportFirstExistingRoot", "/hippo:namespaces"));
            modules.put("exportSecond", singletonList("/"));
            autoExportConfig = new Configuration(true, modules, null, null);
        }

        public MergeFixture base(final String... base) {
            this.base = base;
            return this;
        }

        public MergeFixture toExport(final String... toExport) {
            this.toExport = toExport;
            return this;
        }

        public MergeFixture config(final Configuration config) {
            this.autoExportConfig = config;
            return this;
        }

        public void test() throws Exception {
            try {
                log.debug("\n\nRunning test: {}", testName);

                final ConfigurationModelImpl model = new ConfigurationModelImpl();

                // build base
                for (String baseName : base) {
                    model.addModule(loadModule(in(testName, baseName)));
                }

                // build auto-export targets
                for (String toExportName : toExport) {
                    final ModuleImpl toExportModule = loadModule(in(testName, toExportName));
                    toExportModule.setMvnPath(toExportName);
                    model.addModule(toExportModule);
                }
                model.build();

                // load diff module
                final ModuleImpl diff = loadModule(in(testName, "diff"));

                // merge diff
                DefinitionMergeService merger = new DefinitionMergeService(autoExportConfig);
                Collection<ModuleImpl> allMerged =
                        merger.mergeChangesToModules(diff, new EventJournalProcessor.Changes(), model);

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
                    .resolve("../../out/"+module.getName());
        }

        /**
         * Load a module given a root path in the resources for this test class.
         */
        protected ModuleImpl loadModule(String modulePath) throws Exception {
            final PathConfigurationReader.ReadResult result =
                    readFromResource(modulePath + "/" + Constants.HCM_MODULE_YAML);

            return result.getGroups().stream()
                    .flatMap(group -> group.getProjects().stream())
                    .flatMap(project -> project.getModules().stream())
                    .findFirst().get();
        }

        protected void writeAndCompare(String testName, ModuleImpl module) throws Exception {
            final FileConfigurationWriter writer = new FileConfigurationWriter();

            // write module to a new temp dir
            Path mOut = output.newFolder(module.getName()).toPath();
            final ModuleContext moduleContext = new ModuleContext(module, in(testName, module));
            moduleContext.createOutputProviders(mOut);
            writer.writeModule(module, DEFAULT_EXPLICIT_SEQUENCING, moduleContext);

            // in case we expect an empty output for a module, we need to create the empty dir to compare against,
            // since the normal mvn resource copy will not recreate empty dirs in target
            final Path expectedRoot = out(testName, module);
            if (!expectedRoot.toFile().exists()) {
                expectedRoot.toFile().mkdir();
            }

            // compare output to expected
            assertNoFileDiff(testName+": "+module.getName(), expectedRoot, mOut);
        }
    }
}
