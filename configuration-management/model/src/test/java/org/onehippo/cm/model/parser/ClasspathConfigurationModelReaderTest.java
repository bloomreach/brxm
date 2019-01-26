/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.parser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModelTestUtils;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.path.JcrPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.joor.Reflect.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ClasspathConfigurationModelReaderTest extends AbstractBaseTest {

    private static final Logger log = LoggerFactory.getLogger(ClasspathConfigurationModelReaderTest.class);

    @Test
    public void load_shared_modules() throws IOException {
        //Create shared classloader containing cms & site modules, and dedicated cms & site classloaders.
        final Path rootPath = Paths.get(getClass().getClassLoader().getResource("hcm-module-site.yaml").getFile()).getParent();
        final Path classloadersPath = rootPath.resolve("classloaders");

        final URL sharedSiteURL = classloadersPath.resolve("shared-site").toUri().toURL();
        final URL sharedCMSURL = classloadersPath.resolve("shared-cms").toUri().toURL();

        final URL cmsURL = classloadersPath.resolve("cms").toUri().toURL();
        final URL siteURL = classloadersPath.resolve("site").toUri().toURL();

        final ClassLoader sharedClassloader = new URLClassLoader(new URL[]{sharedSiteURL, sharedCMSURL}, null);
        final ClassLoader cmsClassloader = new URLClassLoader(new URL[]{cmsURL}, sharedClassloader);
        final ClassLoader siteClassloader = new URLClassLoader(new URL[]{siteURL}, sharedClassloader);

        ClasspathConfigurationModelReader classpathReader = new ClasspathConfigurationModelReader();

        // load the cms model and the main dev but not site dev should be present
        List<ModuleImpl> cmsModuleList =  on(classpathReader).call("readModulesFromClasspath",
                cmsClassloader, false, null, JcrPaths.getPath("/hst:hst")).call("getRight").get();
        cmsModuleList.sort(Comparator.comparing(ModuleImpl::getName));
        assertEquals(2, cmsModuleList.size());
        assertEquals("app-module", cmsModuleList.get(0).getName());
        assertEquals("dev-module", cmsModuleList.get(1).getName());

        // load the site model and the site dev but not the main dev should be present
        List<ModuleImpl> siteModuleList =  on(classpathReader).call("readModulesFromClasspath",
                siteClassloader, false, "mainsite", JcrPaths.getPath("/hst:hst")).call("getRight").get();
        siteModuleList.sort(Comparator.comparing(ModuleImpl::getName));

        assertEquals(2, siteModuleList.size());
        assertEquals("site-dev-module", siteModuleList.get(0).getName());
        assertEquals("site-module", siteModuleList.get(1).getName());
    }


    @Test
    public void load_modules_from_classpath() throws IOException, ParserException, URISyntaxException {
        Set<ModuleImpl> classpathModules = loadModules();

        assertEquals("Classpath should contain correct number of modules", 1,
                classpathModules.size());

        Set<ModuleImpl> expectedModules = new HashSet<>();
        final ModuleImpl testModule = ModelTestUtils.makeModule("test-module");
        expectedModules.add(testModule);

        assertTrue("Classpath should contain test-module", classpathModules.containsAll(expectedModules));

        ModuleImpl loadedTestModule = classpathModules.stream().filter(module -> module.equals(testModule)).findFirst().get();

        // the test-module loaded from classpath should have a single source with a single definition,
        // since that's what is contained in the files at the root of test-classes
        assertSource(loadedTestModule, "test.yaml", 1);
    }

    /**
     * Helper to load modules using a ClasspathConfigurationModelReader.
     * @return the Set of Modules loaded by the ClasspathConfigurationModelReader with current system properties
     */
    protected Set<ModuleImpl> loadModules() throws IOException, ParserException, URISyntaxException {
        ClasspathConfigurationModelReader classpathReader = new ClasspathConfigurationModelReader();
        ConfigurationModelImpl model = classpathReader.read(getClass().getClassLoader(), true);
        return model.getModulesStream().collect(Collectors.toSet());
    }
}
