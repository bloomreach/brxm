/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.site;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cm.engine.test.IsolatedRepository;
import org.onehippo.cm.engine.test.JcrRunner;
import org.onehippo.cm.engine.test.ModuleInfo;
import org.onehippo.cm.engine.test.Run;
import org.onehippo.cm.engine.test.Validator;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.springframework.mock.web.MockServletContext;

import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.fail;
import static org.onehippo.cm.engine.test.Validator.NOOP;

public class WebFilesReloadingIT {

    static final String WEBFILES_RELOADING_TEST = "WebFilesReloadingTest";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void site_with_webfiles() throws Exception {

        final String fixtureName = "webfiles";
        final String fixture2Name = "webfiles2";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();

        try {
            HippoWebappContext siteContext = createSiteApplicationContext(fixtureName, "sitewithwebfiles");
            hippoWebappContextRegistry.register(siteContext);

            fixture.test(session -> {
                try {
                    session.getNode("/webfiles/webfilebundle/css/test.css");
                } catch (PathNotFoundException ex) {
                    fail("Initial import of webfiles failed");
                }

                hippoWebappContextRegistry.unregister(siteContext);
                session.logout();
                IsolatedRepository repository = fixture.getRepository();

                //Register same site with different webfiles while repository is stopped
                repository.stop();
                hippoWebappContextRegistry.register(createSiteApplicationContext(fixture2Name, "sitewithwebfiles"));

                repository.startRepository();
                session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

                try {
                    session.getNode("/webfiles/webfilebundle/css/test2.css");
                } catch (PathNotFoundException ex) {
                    fail("Webfiles should bootstrap when changed, even when there are no changes in config or content");
                }
                session.logout();
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    @Test
    public void site_with_webfiles_loaded_after_repo() throws Exception {

        final String fixtureName = "webfiles";
        final String fixture2Name = "webfiles2";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();

        try {
            HippoWebappContext siteContext = createSiteApplicationContext(fixtureName, "sitewithwebfiles");
            hippoWebappContextRegistry.register(siteContext);

            fixture.test(session -> {
                try {
                    session.getNode("/webfiles/webfilebundle/css/test.css");
                } catch (PathNotFoundException ex) {
                    fail("Initial import of webfiles failed");
                }

                hippoWebappContextRegistry.unregister(siteContext);
                session.logout();

                IsolatedRepository repository = fixture.getRepository();
                repository.stop();

                //Register same site with different webfiles after repository is started
                repository.startRepository();
                hippoWebappContextRegistry.register(createSiteApplicationContext(fixture2Name, "sitewithwebfiles"));

                session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
                try {
                    session.getNode("/webfiles/webfilebundle/css/test2.css");
                } catch (PathNotFoundException ex) {
                    fail("Webfiles should bootstrap when changed, even when there are no changes in config or content");
                }
                session.logout();
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    private static Set<String> sharedClasses = ImmutableSet.of(
            "org.onehippo.cm.engine.Configuration",
            "org.hippoecm.repository.",
            "org.apache.jackrabbit",
            "org.onehippo.repository.",
            "org.quartz.spi.",
            "org.onehippo.cm.",
            "org.onehippo.cms7.services.",
            "org.onehippo.cms7.services.webfiles.");

    public HippoWebappContext createSiteApplicationContext(final String fixtureName, final String siteName) throws MalformedURLException {
        final Path siteBasePath = getSiteBasePath(fixtureName, getBaseDir());
        final Path sitePath = siteBasePath.resolve(siteName);
        final URL dirUrl = new URL(sitePath.toUri().toURL().toString());
        final URLClassLoader siteClassLoader = new URLClassLoader(new URL[]{dirUrl}, null);
        return new HippoWebappContext(HippoWebappContext.Type.SITE,
                new SiteServletContext(sitePath.toString(), "/"+siteName, siteClassLoader));
    }

    public Path getSiteBasePath(final String fixtureName, final Path path) {
        return path.resolve("src")
                .resolve("test")
                .resolve("resources")
                .resolve(WEBFILES_RELOADING_TEST)
                .resolve(fixtureName)
                .resolve("site");
    }

    public Path getBaseDir() {
        String basedir = System.getProperty("basedir");
        basedir = basedir != null ? basedir : System.getProperty("user.dir");
        return Paths.get(basedir);
    }

    private class Fixture extends AbstractBaseTest {

        private final ModuleInfo[] modules;
        private final Path projectPath;
        private Set<String> sharedClasses = new HashSet<>();

        public Fixture(final String fixtureName) throws IOException {
            this(fixtureName, WebFilesReloadingIT.sharedClasses);
        }

        public Fixture(final String fixtureName, final Set<String> sharedClasses) throws IOException {
            this(new WebFilesReloadingSiteModuleInfo(fixtureName));
            if (sharedClasses != null) {
                this.sharedClasses.addAll(sharedClasses);
            }
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
                                this.sharedClasses, false);

                repository.startRepository();
                final Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

                run.getPreConditionValidator().validate(session, repository.getRuntimeConfigurationModel());
                run.getJcrRunner().run(session);
                run.getPostConditionValidator().validate(session, repository.getRuntimeConfigurationModel());

                if(session.isLive()){
                    session.logout();
                }

                repository.stop();
            }
        }
    }

    private static class SiteServletContext extends MockServletContext {

        private final String resourcePath;
        private final ClassLoader classLoader;

        public SiteServletContext(final String resourcePath, final String contextPath, final ClassLoader classLoader) {
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
