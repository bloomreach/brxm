/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.TestSettings;
import org.onehippo.cms7.essentials.plugin.sdk.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.sdk.api.service.PlaceholderService;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectServiceImplTest extends ResourceModifyingTest {

    private static final ProjectSettingsBean projectSettings = new ProjectSettingsBean();

    static {
        projectSettings.setSiteModule("test-site");
        projectSettings.setCmsModule("test-cms");
        projectSettings.setRepositoryDataModule("test-repository-data");
        projectSettings.setApplicationSubModule("test-application");
        projectSettings.setDevelopmentSubModule("test-development");
        projectSettings.setWebfilesSubModule("test-webfiles");
        projectSettings.setSelectedBeansPackage("com.test.bean");
        projectSettings.setSelectedRestPackage("com.test.rest");
        projectSettings.setSelectedComponentsPackage("com.test.component");
    }

    @Inject private TestSettings.Service settingsService;
    @Inject private ProjectServiceImpl projectService;
    @Inject private PlaceholderService placeholderService;
    private final Path projectRoot = Paths.get("/foo/bar");

    @Before
    public void setUp() {
        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, "/foo/bar");
        System.setProperty(EssentialConst.ESSENTIALS_BASEDIR_PROPERTY, "test-essentials");
        settingsService.setSettings(projectSettings);
    }

    @Test
    public void get_base_path() {
        assertEquals(projectRoot, projectService.getBasePathForModule(Module.PROJECT));
        assertEquals(projectRoot.resolve("test-site"), projectService.getBasePathForModule(Module.SITE));
        assertEquals(projectRoot.resolve("test-cms"), projectService.getBasePathForModule(Module.CMS));
        assertEquals(projectRoot.resolve("test-essentials"), projectService.getBasePathForModule(Module.ESSENTIALS));

        final Path repositoryDataRoot = projectRoot.resolve("test-repository-data");
        assertEquals(repositoryDataRoot, projectService.getBasePathForModule(Module.REPOSITORY_DATA));
        assertEquals(repositoryDataRoot.resolve("test-application"), projectService.getBasePathForModule(Module.REPOSITORY_DATA_APPLICATION));
        assertEquals(repositoryDataRoot.resolve("test-development"), projectService.getBasePathForModule(Module.REPOSITORY_DATA_DEVELOPMENT));
        assertEquals(repositoryDataRoot.resolve("test-webfiles"), projectService.getBasePathForModule(Module.REPOSITORY_DATA_WEB_FILES));
    }

    @Test(expected = IllegalArgumentException.class)
    public void get_base_path_invalid() {
        projectService.getBasePathForModule(Module.INVALID);
    }

    @Test
    public void get_pom_path() {
        assertEquals(projectRoot.resolve("test-site").resolve("pom.xml"), projectService.getPomPathForModule(Module.SITE));
    }

    @Test
    public void get_java_root_path() {
        assertEquals(projectRoot.resolve("test-cms").resolve("src").resolve("main").resolve("java"),
                projectService.getJavaRootPathForModule(Module.CMS));
    }

    @Test
    public void get_resources_root_path() {
        assertEquals(projectRoot.resolve("test-essentials").resolve("src").resolve("main").resolve("resources"),
                projectService.getResourcesRootPathForModule(Module.ESSENTIALS));
    }

    @Test
    public void get_webapp_root_path() {
        assertEquals(projectRoot.resolve("test-site").resolve("src").resolve("main").resolve("webapp"),
                projectService.getWebApplicationRootPathForModule(Module.SITE));
    }

    @Test
    public void get_webinf_root_path() {
        assertEquals(projectRoot.resolve("test-site").resolve("src").resolve("main").resolve("webapp").resolve("WEB-INF"),
                projectService.getWebInfPathForModule(Module.SITE));
    }

    @Test
    public void get_beans_root_path() {
        assertEquals(projectRoot.resolve("test-site").resolve("src").resolve("main").resolve("java"),
                projectService.getBeansRootPath());

        final ProjectSettingsBean settings = new ProjectSettingsBean();
        settings.setBeansFolder("beans/src/main/java");
        ((TestSettings.Service)settingsService).setSettings(settings);

        assertEquals(projectRoot.resolve("beans/src/main/java"), projectService.getBeansRootPath());
    }

    @Test
    public void get_beans_package_path() {
        assertEquals(projectRoot.resolve("test-site").resolve("src").resolve("main").resolve("java").resolve("com").resolve("test").resolve("bean"),
                projectService.getBeansPackagePath());

        final ProjectSettingsBean settings = new ProjectSettingsBean();
        settings.setBeansFolder("beans/src/main/java");
        settings.setSelectedBeansPackage("com.test.bean");
        ((TestSettings.Service)settingsService).setSettings(settings);

        assertEquals(projectRoot.resolve("beans/src/main/java").resolve("com").resolve("test").resolve("bean"),
                projectService.getBeansPackagePath());
    }

    @Test
    public void get_rest_package_path() {
        assertEquals(projectRoot.resolve("test-site").resolve("src").resolve("main").resolve("java").resolve("com").resolve("test").resolve("rest"),
                projectService.getRestPackagePath());
    }

    @Test
    public void get_components_package_path() {
        assertEquals(projectRoot.resolve("test-site").resolve("src").resolve("main").resolve("java").resolve("com").resolve("test").resolve("component"),
                projectService.getComponentsPackagePath());
    }

    @Test
    public void get_context_xml_path() {
        assertEquals(projectRoot.resolve("conf").resolve("context.xml"), projectService.getContextXmlPath());
    }

    @Test
    public void get_assembly_folder_path() {
        assertEquals(projectRoot.resolve("src").resolve("main").resolve("assembly"), projectService.getAssemblyFolderPath());
    }

    @Test
    public void testGetLog4jFiles() throws Exception {
        createModifiableFile("/services/project/empty.txt", "conf/log4j2.xml");
        createModifiableFile("/services/project/empty.txt", "conf/unrelated.xml");
        createModifiableFile("/services/project/empty.txt", "conf/log4j2-foo.xml");
        createModifiableFile("/services/project/empty.txt", "site/log4j2-bar.xml");

        List<String> fileNames = projectService.getLog4j2Files().stream().map(File::getName).collect(Collectors.toList());

        assertEquals(2, fileNames.size());
        assertTrue(fileNames.contains("log4j2.xml"));
        assertTrue(fileNames.contains("log4j2-foo.xml"));
    }

    @Test
    public void copy_resource() throws Exception {
        final String resourcePath = "/services/project/to-be-copied.txt";
        final String targetLocation = "{{" + PlaceholderService.PROJECT_ROOT + "}}/test/copy.txt";

        // resets the project root
        createModifiableDirectory("test");
        final Map<String, Object> placeholderData = placeholderService.makePlaceholders();
        placeholderData.put("key", "value");

        assertTrue(projectService.copyResource(resourcePath, targetLocation, placeholderData, false, false));

        // validate targetLocation interpolation and data interpolation
        final File copied = getModifiableFile("test/copy.txt");
        assertTrue(contentOf(copied).contains("value"));

        // cannot do is again if overwrite is false
        assertFalse(projectService.copyResource(resourcePath, targetLocation, placeholderData, false, false));

        // can do is again if overwrite is true
        assertTrue(projectService.copyResource(resourcePath, targetLocation, placeholderData, true, true));

        // no data interpolation in binary mode
        final String binaryContent = contentOf(copied);
        assertTrue(binaryContent.contains("{{key}}"));
        assertFalse(binaryContent.contains("value"));

        // and delete again
        assertTrue(copied.exists());
        assertTrue(projectService.deleteFile(targetLocation, placeholderData));
        assertFalse(copied.exists());
    }

    @Test
    public void copy_absent_resource() throws Exception {
        final String resourcePath = "/services/project/absent.txt";
        final String targetLocation = "{{" + PlaceholderService.PROJECT_ROOT + "}}" + File.separator + "test" + File.separator + "copy.txt";
        final Map<String, Object> placeholderData = placeholderService.makePlaceholders();

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(ProjectServiceImpl.class).build()) {
            assertFalse(projectService.copyResource(resourcePath, targetLocation, placeholderData, false, false));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to access resource '/services/project/absent.txt'.")));
        }
    }

    @Test
    public void copy_onto_directory() throws Exception {
        final String resourcePath = "/services/project/to-be-copied.txt";
        final String targetLocation = "{{" + PlaceholderService.PROJECT_ROOT + "}}" + File.separator + "test";
        final Map<String, Object> placeholderData = placeholderService.makePlaceholders();
        createModifiableDirectory("test");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(ProjectServiceImpl.class).build()) {
            assertFalse(projectService.copyResource(resourcePath, targetLocation, placeholderData, true, false));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to copy file from '/services/project/to-be-copied.txt' to")));
        }
    }

    @Test
    public void delete_non_empty_dir() throws Exception {
        final String resourcePath = "/services/project/to-be-copied.txt";
        final String targetLocation = "{{" + PlaceholderService.PROJECT_ROOT + "}}" + File.separator + "test";
        createModifiableFile(resourcePath, "test/file.txt");
        final Map<String, Object> placeholderData = placeholderService.makePlaceholders();

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(ProjectServiceImpl.class).build()) {
            assertFalse(projectService.deleteFile(targetLocation, placeholderData));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Failed to deleting file")));
        }
    }
}
