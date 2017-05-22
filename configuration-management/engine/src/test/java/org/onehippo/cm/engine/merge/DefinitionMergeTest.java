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
package org.onehippo.cm.engine.merge;

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
import org.onehippo.cm.engine.Constants;
import org.onehippo.cm.engine.FileConfigurationWriter;
import org.onehippo.cm.engine.ModuleContext;
import org.onehippo.cm.engine.PathConfigurationReader;
import org.onehippo.cm.engine.SerializerTest;
import org.onehippo.cm.impl.model.ConfigurationModelImpl;
import org.onehippo.cm.impl.model.GroupImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.builder.ConfigurationModelBuilder;
import org.onehippo.cms7.autoexport.AutoExportConfigFactory;
import org.onehippo.cms7.autoexport.Configuration;
import org.onehippo.cms7.autoexport.DefinitionMergeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonList;
import static org.onehippo.cm.engine.Constants.DEFAULT_EXPLICIT_SEQUENCING;

public class DefinitionMergeTest {

    private static final Logger log = LoggerFactory.getLogger(DefinitionMergeTest.class);

    @Rule
    public TemporaryFolder output = new TemporaryFolder();

    @Test
    public void template_works() throws Exception {
        new MergeTest("template").test();
    }

    @Test
    public void created_and_updated_namespaces() throws Exception {
        new MergeTest("namespace").test();
    }

    @Test
    public void new_node() throws Exception {
        new MergeTest("new-node").test();
    }

//    @Test
//    public void delete_node() throws Exception {
//        new MergeTest("delete-node").test();
//    }

    public class MergeTest extends SerializerTest {
        String testName;
        String[] base = {"topmost", "upstream"};
        String[] toExport = {"exportFirst", "exportSecond"};
        Configuration autoExportConfig;

        public MergeTest(final String testName) {
            this.testName = testName;

            Map<String, Collection<String>> modules = new HashMap<String, Collection<String>>();
            modules.put("exportFirst", Arrays.asList("/topmost", "/exportFirstExistingRoot", "/hippo:namespaces"));
            modules.put("exportSecond", singletonList("/"));
            autoExportConfig = AutoExportConfigFactory.make(true, modules, null, null);
        }

        public MergeTest base(final String... base) {
            this.base = base;
            return this;
        }

        public MergeTest toExport(final String... toExport) {
            this.toExport = toExport;
            return this;
        }

        public MergeTest config(final Configuration config) {
            this.autoExportConfig = config;
            return this;
        }

        public void test() throws Exception {
            try {
                log.debug("\n\nRunning test: {}", testName);

                final ConfigurationModelBuilder builder = new ConfigurationModelBuilder();

                // build base
                for (String baseName : base) {
                    push(builder, loadModule(in(testName, baseName)));
                }

                // build auto-export targets
                final Set<ModuleImpl> toExportModules = new HashSet<>(toExport.length);
                for (String toExportName : toExport) {
                    final ModuleImpl toExportModule = loadModule(in(testName, toExportName));
                    toExportModule.setMvnPath(toExportName);
                    toExportModules.add(toExportModule);
                    push(builder, toExportModule);
                }
                ConfigurationModelImpl model = (ConfigurationModelImpl) builder.build();

                // load diff module
                final ModuleImpl diff = loadModule(in(testName, "diff"));

                // merge diff
                DefinitionMergeService merger = new DefinitionMergeService(autoExportConfig);
                Collection<ModuleImpl> allMerged = merger.mergeChangesToModules(diff, toExportModules, model);

                for (ModuleImpl merged : allMerged) {
                    writeAndCompare(testName, merged);
                }
            }
            catch (Exception e) {
                e.printStackTrace(System.err);
                throw e;
            }
        }

        protected void push(ConfigurationModelBuilder builder, ModuleImpl module) {
            builder.push((GroupImpl) module.getProject().getGroup());
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

            return result.getGroups().values().stream()
                    .flatMap(group -> group.getModifiableProjects().stream())
                    .flatMap(project -> project.getModifiableModules().stream())
                    .findFirst().get();
        }

        protected void writeAndCompare(String testName, ModuleImpl module) throws Exception {
            final FileConfigurationWriter writer = new FileConfigurationWriter();

            // write module to a new temp dir
            Path mOut = output.newFolder(module.getName()).toPath();
            final ModuleContext moduleContext = new ModuleContext(module, in(testName, module));
            moduleContext.createOutputProviders(mOut);
            writer.writeModule(module, DEFAULT_EXPLICIT_SEQUENCING, moduleContext);

            // compare output to expected
            assertNoFileDiff(testName+": "+module.getName(), out(testName, module), mOut);
        }
    }
}
