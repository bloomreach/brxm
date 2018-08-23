/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cm.engine.autoexport.IsolatedRepository;
import org.onehippo.cm.engine.autoexport.JcrRunner;
import org.onehippo.cm.engine.autoexport.ModuleInfo;
import org.onehippo.cm.engine.autoexport.Run;
import org.onehippo.cm.engine.autoexport.Validator;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationTreeBuilder;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.springframework.mock.web.MockServletContext;

import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED;
import static org.onehippo.cm.engine.autoexport.Validator.NOOP;

public class ExtensionIntegrationTest {

    static final String EXTENSIONS_INTEGRATION_TEST = "ExtensionsIntegrationTest";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void extensions_before_and_after_cms_init() throws Exception {

        final String fixtureName = "extensions_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                final IsolatedRepository repository = fixture.getRepository();

                try {
                    final Node extNode = session.getNode("/m1-extension");
                    extNode.getProperty("property").getString();
                } catch (PathNotFoundException ex) {
                    fail("Node extension-m1 from extension m1 or its properties are not found");
                }

                try {
                    session.getNode("/m2-extension");
                    fail("Extension node should not exist");
                } catch (PathNotFoundException e) {
                    //expected
                }

                //Validate that baseline contains m1 extension after core bootstrap
                final ConfigurationModelImpl runtimeModel1 = repository.getRuntimeConfigurationModel();
                final ConfigurationNodeImpl configurationNode = runtimeModel1.resolveNode("/m1-extension");
                assertNotNull(configurationNode);

                hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m2"));

                try {
                    final Node extNode2 = session.getNode("/m2-extension");
                    extNode2.getProperty("property").getString();
                } catch (PathNotFoundException ex) {
                    fail("Node extension-m2 from extension m2 or its properties are not found");
                }

                //Validate that baseline contains m1 & m2 extensions after extension event
                final ConfigurationModelImpl runtimeModel2 = repository.getRuntimeConfigurationModel();

                final ConfigurationNodeImpl configurationNode1 = runtimeModel2.resolveNode("/m1-extension");
                assertNotNull(configurationNode1);

                final ConfigurationNodeImpl configurationNode2 = runtimeModel2.resolveNode("/m2-extension");
                assertNotNull(configurationNode2);

            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }

    }

    @Test
    public void extensions_test_incompatible_nodeoverride() throws Exception {

        final String fixtureName = "extensions_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m2"));
                try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
                    try {
                        hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m3"));
                        fail("Failure is expected since node from extension 'A' cannot override node from extension 'B'");
                    } catch (Exception ignore) {
                        assertTrue(interceptor.messages()
                                .anyMatch(m->m.startsWith("Cannot merge config definitions with the same path '/m2-extension' " +
                                        "defined in different extensions or in both core and an extension")));
                    }
                }
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    @Test
    public void extensions_test_incompatible_subnode() throws Exception {

        final String fixtureName = "extensions_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m2"));
                try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
                    try {
                        hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m4"));
                        fail("Failure is expected since extension's node cannot belong to node from different extension");
                    } catch (Exception ignore) {
                        assertTrue(interceptor.messages()
                                .anyMatch(m->m.startsWith("Cannot add child config definition '/m2-extension/extension4node' to parent node definition")));
                    }
                }
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    @Test
    public void extensions_test_delete() throws Exception {

        //Delete node which is part of another extension
        final String fixtureName = "extensions_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m2"));
                try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
                    try {
                        hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m5"));
                        fail("Failure is expected since extension cannot delete node which belongs to another extension or core");
                    } catch (Exception ignore) {
                        assertTrue(interceptor.messages()
                                .anyMatch(m->m.startsWith("Cannot merge config definitions with the same path '/corenode/subcorenode' " +
                                        "defined in different extensions or in both core and an extension")));
                    }
                }
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    /**
     * Apply content definition '/content/m2contentnode/m6node' from m6 extension
     * on top of m2 extension '/content/m2contentnode'.
     */
    @Test
    public void extensions_test_incompatible_content_defs() throws Exception {

        System.setProperty(SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED, "false");
        final String fixtureName = "extensions_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m2"));
                hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m6"));
                try {
                    final Node m2node = session.getNode("/content/m2contentnode");
                    if (m2node.hasNode("m6node")) {
                        fail("Node '/content/m2contentnode/m6node' should not exist");
                    }
                } catch(PathNotFoundException ignore) {
                    //Expected
                }
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    @Test
    public void extensions_test_namespace_defs() throws Exception {

        System.setProperty(SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED, "false");
        final String fixtureName = "extensions_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "m2"));
                try {
                    hippoWebappContextRegistry.register(createExtensionApplicationContext(fixtureName, "namespace"));
                    fail("Namespace definitions from extension modules are not supported");
                } catch(Exception ex) {
                    assertTrue(ex.getMessage().contains("Namespace definition can not be a part of extension module"));
                }
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    public HippoWebappContext createExtensionApplicationContext(final String fixtureName, final String extensionName) throws MalformedURLException {
        final Path extensionsBasePath = getExtensionBasePath(fixtureName, getBaseDir());
        final Path extensionPath = extensionsBasePath.resolve(extensionName);
        final URL dirUrl = new URL(extensionPath.toUri().toURL().toString());
        final URLClassLoader extensionClassLoader = new URLClassLoader(new URL[]{dirUrl}, null);
        return new HippoWebappContext(HippoWebappContext.Type.SITE,
                new ExtensionServletContext(extensionPath.toString(), "/"+extensionName, extensionClassLoader));
    }

    public Path getExtensionBasePath(final String fixtureName, final Path path) {
        return path.resolve("src")
                .resolve("test")
                .resolve("resources")
                .resolve(EXTENSIONS_INTEGRATION_TEST)
                .resolve(fixtureName)
                .resolve("extension");
    }

    public Path getBaseDir() {
        String basedir = System.getProperty("basedir");
        basedir = basedir != null ? basedir : System.getProperty("user.dir");
        return Paths.get(basedir);
    }

    private class Fixture extends AbstractBaseTest {

        private final ModuleInfo[] modules;
        private final Path projectPath;

        public Fixture(final String fixtureName) throws IOException {
            this(new ExtensionModuleInfo(fixtureName));
        }

        private Fixture(final ModuleInfo... modules) throws IOException {
            this.projectPath = folder.newFolder("project").toPath();
            this.modules = modules;
        }

        public void test(final JcrRunner jcrRunner) throws Exception {
            test(NOOP, jcrRunner, NOOP);
        }

        public void test(final Validator preConditionValidator,
                  final JcrRunner jcrRunner,
                  final Validator postConditionValidator) throws Exception {
            run(new Run(modules, preConditionValidator, jcrRunner, postConditionValidator));
        }

        IsolatedRepository repository;

        public IsolatedRepository getRepository() {
            return repository;
        }

        public void run(final Run... runs) throws Exception {
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

                repository =
                        new IsolatedRepository(folder.getRoot(), projectPath.toFile(), additionalClasspathURLs,
                                ImmutableSet.of("org.onehippo.cms7.services."), false);

                repository.startRepository();
                final Session session = repository.login(IsolatedRepository.CREDENTIALS);

                run.getPreConditionValidator().validate(session, repository.getRuntimeConfigurationModel());
                run.getJcrRunner().run(session);
                run.getPostConditionValidator().validate(session, repository.getRuntimeConfigurationModel());

                session.logout();
                repository.stop();
            }
        }
    }

    private static class ExtensionServletContext extends MockServletContext {

        private final String resourcePath;
        private final ClassLoader classLoader;

        public ExtensionServletContext(final String resourcePath, final String contextPath, final ClassLoader classLoader) {
            super(resourcePath);
            this.resourcePath = resourcePath;
            this.classLoader = classLoader;
            setContextPath(contextPath);
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        public InputStream getResourceAsStream(String path) {
            File file = new File(resourcePath, path);
            if (file.exists() && file.isFile()) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("Path "+path+" not found");
                }
            }
            return null;
        }
    }
}
