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

import java.io.IOException;
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
import javax.servlet.ServletContext;

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
import org.onehippo.cms7.services.eventbus.GuavaHippoEventBus;
import org.onehippo.cms7.services.extension.ExtensionEvent;
import org.onehippo.cms7.services.extension.ExtensionRegistry;
import org.springframework.mock.web.MockServletContext;

import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.fail;

public class ExtensionIntegrationTest {

    public static final String EXTENSIONS_INTEGRATION_TEST = "ExtensionsIntegrationTest";
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final GuavaHippoEventBus eventBus = new GuavaHippoEventBus();

    @Test
    public void extensions_before_and_after_cms() throws Exception {

        final String fixtureName = "extensions_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final ExtensionEvent event = createExtensionEvent(fixtureName, "m1");

        final ServletContext servletContext = new MockServletContext() {
            @Override
            public String getContextPath() {
                return "/extContext";
            }
        };
        ExtensionRegistry.register(servletContext, event, ExtensionRegistry.ExtensionType.HST);

        fixture.test(session -> {

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

            final ExtensionEvent afterEvent = createExtensionEvent(fixtureName, "m2");
            eventBus.post(afterEvent);
            Thread.sleep(1000); //TODO SS: Replace with while loop or replace with direct call to onNewSiteEvent()

            try {
                final Node extNode2 = session.getNode("/m2-extension");
                extNode2.getProperty("property").getString();
            } catch (PathNotFoundException ex) {
                fail("Node extension-m2 from extension m2 or its properties are not found");
            }
        });
    }

    public ExtensionEvent createExtensionEvent(final String fixtureName, final String extensionName) throws MalformedURLException {
        final Path extensionsBasePath = getExtensionBasePath(fixtureName, getBaseDir());
        final Path extensionPath = extensionsBasePath.resolve(extensionName);
        final URL dirUrl = new URL(extensionPath.toUri().toURL().toString());
        final URLClassLoader extensionClassLoader = new URLClassLoader(new URL[]{dirUrl}, null);
        return new ExtensionEvent(extensionName, extensionClassLoader);
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


    private static final Validator NOOP = (session, configurationModel) -> {};

    private class Fixture extends AbstractBaseTest {

        private final ModuleInfo[] modules;
        private final Path projectPath;
        private String fixtureName;

        public Fixture(final String fixtureName) throws IOException {
            this(new ExtensionModuleInfo(fixtureName));
            this.fixtureName = fixtureName;
        }

        private Fixture(final ModuleInfo... modules) throws IOException {
            this.projectPath = folder.newFolder("project").toPath();
            this.modules = modules;
        }

        public String getFixtureName() {
            return fixtureName;
        }

        public void test(final JcrRunner jcrRunner) throws Exception {
            test(NOOP, jcrRunner, NOOP);
        }

        public void test(final Validator preConditionValidator,
                  final JcrRunner jcrRunner,
                  final Validator postConditionValidator) throws Exception {
            run(new Run(modules, preConditionValidator, jcrRunner, postConditionValidator));
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

                final IsolatedRepository repository =
                        new IsolatedRepository(folder.getRoot(), projectPath.toFile(), additionalClasspathURLs,
                                ImmutableSet.of("org.onehippo.cms7.services."));

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
}
