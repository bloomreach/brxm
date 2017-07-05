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
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Predicate;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cm.ConfigurationService;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModelTestUtils;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertTrue;
import static org.onehippo.cm.model.util.FilePathUtils.nativePath;

@Ignore
public class FilesystemModuleTest {

    // We need to init some properties before the static setup in RepositoryTestCase, so we have to work around
    // the normal init order that would happen if we directly extend RepositoryTestCase here.
    @BeforeClass
    public static void beforeClass() throws Exception {
        // set system properties to trigger developer mode that loads config from source files
        System.setProperty("project.basedir", calculateBaseDir() + nativePath("/src/test/resources"));
        System.setProperty(AutoExportConstants.SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED, "true");
        System.setProperty(AutoExportConstants.SYSTEM_PROPERTY_AUTOEXPORT_ENABLED, "false");

        RepositoryTestCase.setUpClass();
    }

    // We have to explicitly clean up the repository, because we reuse RepositoryTestCase without extending it.
    @AfterClass
    public static void afterClass() throws Exception {
        RepositoryTestCase.tearDownClass();
    }

    @Test
    public void load_modules_from_classpath_and_filesystem() throws IOException, ParserException, URISyntaxException {
        // We expect the config that's stored in the engine module's src/test/resources/hcm-config/main.yaml
        // to set the proper auto-export module config when the bootstrap config is loaded from the classpath.
        ConfigurationService configService = HippoServiceRegistry.getService(ConfigurationService.class);
        final ConfigurationModelImpl cfg = (ConfigurationModelImpl) configService.getRuntimeConfigurationModel();
        final ModuleImpl testModule = ModelTestUtils.makeModule("test-module");

        final Optional<ModuleImpl> maybeTestModule =
                cfg.getModulesStream().filter(Predicate.isEqual(testModule)).findFirst();

        assertTrue("Configuration should contain test-module", maybeTestModule.isPresent());
        ModuleImpl loadedTestModule = maybeTestModule.get();

        // the test-module loaded from source files should have a single source with two definitions, not one,
        // since that's what is contained in the files under TestModuleFileSource
        AbstractBaseTest.assertSource(loadedTestModule, "test.yaml", 2);
    }

    /**
     * Utility method to calculate correct path in case when run under Intellij IDEA (Working directory should be set to
     * module's root, e.g. ../master/engine)
     * @return base directory
     */
    private static String calculateBaseDir() {
        String basedir = System.getProperty("basedir");
        basedir = basedir != null ? basedir: System.getProperty("user.dir");
        return basedir;
    }
}
