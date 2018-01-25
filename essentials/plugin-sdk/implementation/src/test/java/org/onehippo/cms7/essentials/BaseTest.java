/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.onehippo.cms7.essentials.plugin.sdk.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.test.ApplicationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @version "$Id$"
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {ApplicationModule.class})
@ActiveProfiles("settings-test")
public abstract class BaseTest {
    public static final String PROJECT_NAMESPACE_TEST = "testnamespace";

    static final ProjectSettingsBean projectSettings = new ProjectSettingsBean();

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    private static final String TEST_PROJECT_PACKAGE = "org.onehippo.cms7.essentials.dashboard.test";

    static {
        projectSettings.setProjectNamespace(PROJECT_NAMESPACE_TEST);
        projectSettings.setSelectedProjectPackage(TEST_PROJECT_PACKAGE);
        projectSettings.setSelectedBeansPackage(TEST_PROJECT_PACKAGE + ".beans");
        projectSettings.setSelectedComponentsPackage(TEST_PROJECT_PACKAGE + ".components");
        projectSettings.setSelectedRestPackage(TEST_PROJECT_PACKAGE + ".rest");
    }

    @Inject protected AutowireCapableBeanFactory injector;
    @Inject private TestSettings.Service settingsService;

    public static final Set<String> NAMESPACES_TEST_SET = new ImmutableSet.Builder<String>()
            .add("hippoplugins:extendingnews")
            .add("myproject:newsdocument")
            .add("hippoplugins:extendedbase")
            .add("hippoplugins:textdocument")
            .add("hippoplugins:basedocument")
            .add("hippoplugins:plugin")
            .add("hippoplugins:vendor")
            .add("hippoplugins:newsdocument")
            .add("hippoplugins:version")
            .add("hippoplugins:dependency")
            .build();
    private Path projectRoot;
    private boolean hasProjectStructure;

    public void setProjectRoot(final Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    @After
    public void tearDown() throws Exception {
        // delete project files:
        if (projectRoot != null) {
            final File file = projectRoot.toFile();
            if (file.exists()) {
                try {
                    FileUtils.deleteDirectory(file);
                } catch (IOException e) {
                    log.error("Error deleting file {}, {}", file.getPath(),e.getMessage());
                }
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        settingsService.setSettings(projectSettings);

        // create temp dir:
        final String tmpDir = System.getProperty("java.io.tmpdir");
        final File root = new File(tmpDir);
        final File projectRootDir = new File(root.getAbsolutePath() + File.separator + "project");
        if (!projectRootDir.exists()) {
            projectRootDir.mkdir();
        }
        projectRoot = projectRootDir.toPath();
        ensureProjectStructure();
    }

    /**
     * Plugin context with file system support
     *
     * @return PluginContext with file system initialized (so no JCR session)
     */
    public void ensureProjectStructure() {
        if (!hasProjectStructure) {

            final String basePath = projectRoot.toString();
            System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, basePath);
            final File file = new File(basePath);
            if (file.exists()) {
                final File cmsFolder = new File(basePath + File.separator + "cms");
                if (!cmsFolder.exists()) {
                    cmsFolder.mkdir();
                }
                final File siteFolder = new File(basePath + File.separator + "site");
                if (!siteFolder.exists()) {
                    siteFolder.mkdir();
                }
                final File essentialsFolder = new File(basePath + File.separator + "essentials");
                if (!essentialsFolder.exists()) {
                    essentialsFolder.mkdir();
                }
                final File repositoryDataFolder = new File(basePath + File.separator + "repository-data");
                if (!repositoryDataFolder.exists()) {
                    repositoryDataFolder.mkdir();
                }
            }
            hasProjectStructure = true;
        }
    }

    public Path getProjectRoot() {
        return projectRoot;
    }
}
