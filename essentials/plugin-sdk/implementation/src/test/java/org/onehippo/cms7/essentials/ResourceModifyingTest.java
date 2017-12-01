/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import com.google.common.base.Charsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

/**
 * ResourceModifyingTest provides you with an extended PluginContext suitable for testing the modification of
 * project resources without affecting other test cases.
 */
public abstract class ResourceModifyingTest {

    private PluginContext context;
    private String oldProjectBaseDir;
    private Path projectRootPath;

    public PluginContext getContext() throws IOException {
        ensureContext();
        return context;
    }

    @After
    public void after() throws IOException {
        if (context != null) {
            FileUtils.deleteDirectory(projectRootPath.toFile());
            if (oldProjectBaseDir != null) {
                System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, oldProjectBaseDir);
            } else {
                System.clearProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY);
            }
            context = null;
        }
    }

    protected File createModifiableFile(final String resourcePath, final String projectLocation) throws IOException {
        final File output = fileAt(projectLocation);
        final File input = new File(getClass().getResource(resourcePath).getPath());

        FileUtils.copyFile(input, output);

        return output;
    }

    protected File createModifiableDirectory(final String projectLocation) throws IOException {
        final File output = fileAt(projectLocation);

        output.mkdirs();

        return output;
    }

    private File fileAt(final String projectLocation) throws IOException {
        final String[] projectLegs = projectLocation.split("/");
        ensureContext();
        Path outputPath = projectRootPath;
        for (String leg : projectLegs) {
            outputPath = outputPath.resolve(leg);
        }
        return new File(outputPath.toUri());
    }

    protected int nrOfOccurrences(final File file, final String value) throws IOException {
        final String fileContent = com.google.common.io.Files.asCharSource(file, Charsets.UTF_8).read();
        return StringUtils.countMatches(fileContent, value);
    }

    private void ensureContext() throws IOException {
        if (context == null) {
            // create a temporary directory representing the root of modifiable project files
            projectRootPath = Files.createTempDirectory("test");
            oldProjectBaseDir = System.getProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY);
            System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, projectRootPath.toString());

            context = new MockPluginContext();
        }
    }
}
