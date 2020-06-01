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

package org.onehippo.cms7.essentials;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import com.google.common.base.Charsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.test.ApplicationModule;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * ResourceModifyingTest provides you with an extended PluginContext suitable for testing the modification of
 * project resources without affecting other test cases.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {ApplicationModule.class})
@ActiveProfiles("settings-test")
public abstract class ResourceModifyingTest {

    @Inject private TestSettings.Service settingsService;
    @Inject private AutowireCapableBeanFactory injector;

    private String oldProjectBaseDir;
    private Path projectRootPath;

    @Before
    public void setUp() {
        settingsService.setSettings(BaseTest.projectSettings);
    }

    @After
    public void after() throws IOException {
        if (projectRootPath != null) {
            FileUtils.deleteDirectory(projectRootPath.toFile());
            projectRootPath = null;

            if (oldProjectBaseDir != null) {
                System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, oldProjectBaseDir);
            } else {
                System.clearProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY);
            }
        }
    }

    protected File createModifiableFile(final String resourcePath, final String projectLocation) throws IOException {
        final File output = fileAt(projectLocation);
        final File input = new File(getClass().getResource(resourcePath).getPath());

        FileUtils.copyFile(input, output);

        return output;
    }

    protected File getModifiableFile(final String projectLocation) throws IOException {
        return fileAt(projectLocation);
    }

    protected File createModifiableDirectory(final String projectLocation) throws IOException {
        final File output = fileAt(projectLocation);

        output.mkdirs();

        return output;
    }

    private File fileAt(final String projectLocation) throws IOException {
        final String[] projectLegs = projectLocation.split("/");
        ensureModifiableProjectRoot();
        Path outputPath = projectRootPath;
        for (String leg : projectLegs) {
            outputPath = outputPath.resolve(leg);
        }
        return new File(outputPath.toUri());
    }

    protected int nrOfOccurrences(final File file, final String value) throws IOException {
        final String fileContent = contentOf(file);
        return StringUtils.countMatches(fileContent, value);
    }

    protected String contentOf(File file) throws IOException {
        return com.google.common.io.Files.asCharSource(file, Charsets.UTF_8).read();
    }

    protected void autoWire(final Object bean) {
        injector.autowireBean(bean);
    }

    private void ensureModifiableProjectRoot() throws IOException {
        if (projectRootPath == null) {
            // create a temporary directory representing the root of modifiable project files
            projectRootPath = Files.createTempDirectory("test");
            oldProjectBaseDir = System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY);
            System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, projectRootPath.toString());
        }
    }
}
