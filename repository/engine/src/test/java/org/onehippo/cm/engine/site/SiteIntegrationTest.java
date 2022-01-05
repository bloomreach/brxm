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

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cm.engine.ConfigurationContentService;
import org.onehippo.cm.engine.ConfigurationServiceImpl;
import org.onehippo.cm.engine.test.IsolatedRepository;
import org.onehippo.cm.engine.test.JcrRunner;
import org.onehippo.cm.engine.test.ModuleInfo;
import org.onehippo.cm.engine.test.Run;
import org.onehippo.cm.engine.test.Validator;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationTreeBuilder;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.springframework.mock.web.MockServletContext;

import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.cm.engine.Constants.SYSTEM_PARAMETER_REPO_BOOTSTRAP;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED;
import static org.onehippo.cm.engine.test.Validator.NOOP;

public class SiteIntegrationTest {

    static final String SITE_INTEGRATION_TEST = "SiteIntegrationTest";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void sites_with_content() throws Exception {

        final String fixtureName = "sites_with_existing_content";
        final String fixture2Name = "sites_with_existing_content2";
        final String fixture3Name = "sites_with_existing_content3";
        final String fixture4Name = "sites_with_existing_content4";

        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();

        try {
            fixture.test(session -> {
                IsolatedRepository repository = fixture.getRepository();
                //Register site that bootstraps content at same content path
                HippoWebappContext siteContext = createSiteApplicationContext(fixtureName, "sitewithcontent");
                try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(ConfigurationServiceImpl.class).build()) {
                    hippoWebappContextRegistry.register(siteContext);
                    assertEquals("Duplicate definition root paths '/content/sitewithexistingcontent/contentnode' in module 'site-with-content' in source files 'sitewithcontent/hippo-cms-test/site-with-content-project/site-with-content [content: contentnode2.yaml]' and 'sitewithcontent/hippo-cms-test/site-with-content-project/site-with-content [content: contentnode.yaml]'.",
                            interceptor.getEvents().get(0).getThrown().getMessage());
                }
                hippoWebappContextRegistry.unregister(siteContext);
                session.logout();
                repository.stop();

                //Register site that bootstraps normally
                HippoWebappContext siteContext2 = createSiteApplicationContext(fixture2Name, "sitewithcontent");
                hippoWebappContextRegistry.register(siteContext2);
                repository.startRepository();
                session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
                try {
                    session.getNode("/content/sitewithexistingcontent/contentnode").getProperty("property");
                    final String lastAction = session.getNode(
                                    "/hcm:hcm/hcm:baseline/hcm:sites/sitewithcontent/hippo-cms-test/site-with-content-project/site-with-content")
                            .getProperty("hcm:lastexecutedaction").getString();
                    assertEquals("1.1", lastAction);
                    final String lastExecutedModuleAction = repository.getRuntimeConfigurationModel().getModulesStream()
                            .filter(m -> m.getName().equals("site-with-content") && m.isNotCore()).findFirst()
                            .orElseThrow(IllegalArgumentException::new).getLastExecutedAction();
                    assertEquals("1.1", lastExecutedModuleAction);
                } catch (PathNotFoundException ex) {
                    fail("Initial bootstrap failed");
                }
                hippoWebappContextRegistry.unregister(siteContext2);
                session.logout();
                repository.stop();

                //Register site that bootstraps content at a previously loaded content path
                System.setProperty(SYSTEM_PARAMETER_REPO_BOOTSTRAP, "full");
                HippoWebappContext siteContext3 = createSiteApplicationContext(fixture3Name, "sitewithcontent");
                hippoWebappContextRegistry.register(siteContext3);
                repository.startRepository();
                session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
                try {
                    Property property = session.getNode("/content/sitewithexistingcontent/contentnode").getProperty("property");
                    //property should be reloaded
                    assertEquals("propertyValue4", property.getString());
                    final String lastAction = session.getNode(
                                    "/hcm:hcm/hcm:baseline/hcm:sites/sitewithcontent/hippo-cms-test/site-with-content-project/site-with-content")
                            .getProperty("hcm:lastexecutedaction").getString();
                    assertEquals("1.6", lastAction);
                    final String lastExecutedModuleAction = repository.getRuntimeConfigurationModel().getModulesStream()
                            .filter(m -> m.getName().equals("site-with-content") && m.isNotCore()).findFirst()
                            .orElseThrow(IllegalArgumentException::new).getLastExecutedAction();
                    assertEquals("1.6", lastExecutedModuleAction);
                } catch (PathNotFoundException ex) {
                    fail("Unexpected exception");
                }
                hippoWebappContextRegistry.unregister(siteContext3);
                session.logout();
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }


    @Test
    public void sites_before_and_after_cms_init() throws Exception {

        final String fixtureName = "sites_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                final IsolatedRepository repository = fixture.getRepository();

                try {
                    final Node extNode = session.getNode("/m1-extension");
                    extNode.getProperty("property").getString();
                } catch (PathNotFoundException ex) {
                    fail("Node extension-m1 from site m1 or its properties are not found");
                }

                try {
                    session.getNode("/m2-extension");
                    fail("Site node should not exist");
                } catch (PathNotFoundException e) {
                    //expected
                }

                //Validate that baseline contains m1 site after core bootstrap
                final ConfigurationModelImpl runtimeModel1 = repository.getRuntimeConfigurationModel();
                final ConfigurationNodeImpl configurationNode = runtimeModel1.resolveNode("/m1-extension");
                assertNotNull(configurationNode);

                hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m2"));

                try {
                    final Node extNode2 = session.getNode("/m2-extension");
                    extNode2.getProperty("property").getString();
                } catch (PathNotFoundException ex) {
                    fail("Node extension-m2 from site m2 or its properties are not found");
                }

                //Validate that baseline contains m1 & m2 sites after site event
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
    public void sites_test_incompatible_nodeoverride() throws Exception {

        final String fixtureName = "sites_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m2"));
                try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
                    hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m3"));
                    assertTrue(interceptor.messages()
                            .anyMatch(m->m.startsWith("Cannot merge config definitions with the same path '/m2-extension' " +
                                    "defined in different sites or in both core and a site")));
                }
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    @Test
    public void sites_test_incompatible_subnode() throws Exception {

        final String fixtureName = "sites_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m2"));
                try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
                    hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m4"));
                    assertTrue(interceptor.messages()
                            .anyMatch(m->m.startsWith("Cannot add child config definition '/m2-extension/extension4node' to parent node definition")));
                }
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    @Test
    public void sites_test_delete() throws Exception {

        //Delete node which is part of another site
        final String fixtureName = "sites_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m2"));
                try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(ConfigurationTreeBuilder.class).build()) {
                    hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m5"));
                    assertTrue(interceptor.messages()
                            .anyMatch(m->m.startsWith("Cannot merge config definitions with the same path '/corenode/subcorenode' " +
                                    "defined in different sites or in both core and a site")));
                }
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    /**
     * Apply content definition '/content/m2contentnode/m6node' from m6 site
     * on top of m2 site '/content/m2contentnode'.
     */
    @Test
    public void sites_test_incompatible_content_defs() throws Exception {

        System.setProperty(SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED, "false");
        final String fixtureName = "sites_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m2"));
                final HippoWebappContext m6App = createSiteApplicationContext(fixtureName, "m6");

                try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(ConfigurationContentService.class).build()) {
                    hippoWebappContextRegistry.register(m6App);
                    try {
                        final Node m2node = session.getNode("/content/m2contentnode");
                        if (m2node.hasNode("m6node")) {
                            fail("Node '/content/m2contentnode/m6node' should not exist");
                        }
                    } catch (PathNotFoundException ignore) {
                        //Expected
                    }
                    assertTrue(interceptor.messages().anyMatch(m -> m.equals("Incompatible content definition: /content/m2contentnode/m6node. Content definition " +
                            "can be applied only if it's parent belongs to core or same extension")));
                }
            });
        } finally {
            hippoWebappContextRegistry.getEntries().forEach(e -> HippoWebappContextRegistry.get().unregister(e.getServiceObject()));
        }
    }

    @Test
    public void sites_test_namespace_defs() throws Exception {

        System.setProperty(SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED, "false");
        final String fixtureName = "sites_before_cms";
        final Fixture fixture = new Fixture(fixtureName);

        final HippoWebappContextRegistry hippoWebappContextRegistry = HippoWebappContextRegistry.get();
        try {
            hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m1"));
            fixture.test(session -> {

                hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "m2"));
                try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(ConfigurationServiceImpl.class).build()) {
                    hippoWebappContextRegistry.register(createSiteApplicationContext(fixtureName, "namespace"));
                    final String expectedErrorMessage = "Namespace definition can not be a part of site module";
                    assertTrue(interceptor.getEvents().get(0).getThrown().getMessage().contains(expectedErrorMessage));
                }
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
                .resolve(SITE_INTEGRATION_TEST)
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
            this(fixtureName, SiteIntegrationTest.sharedClasses);
        }

        public Fixture(final String fixtureName, final Set<String> sharedClasses) throws IOException {
            this(new SiteModuleInfo(fixtureName));
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
