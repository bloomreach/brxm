/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConfigurationParserTest {

    private URL pathToUrl(final Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            fail("Cannot convert path to URL" + e);
            return null;
        }
    }

    private List<URL> collectFiles(final String resourceName) throws IOException {
        final URL url = ConfigurationParserTest.class.getResource(resourceName);
        if (url == null) {
            fail("cannot find resource " + resourceName);
        }
        final String fileName = url.getFile();
        final String[] parts = fileName.split("/");
        if (parts.length < 3) {
            fail("resource file name must be composed of 3 or more elements, found only " + parts.length + " elements in " + fileName);
        }
        final List<URL> result = new ArrayList<>();
        final Path testRootDirectory = Paths.get("/" + parts[0], Arrays.copyOfRange(parts, 1, parts.length - 3));
        Files.find(testRootDirectory, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                .forEachOrdered(path -> result.add(pathToUrl(path)));
        return result;
    }

    @Ignore
    @Test
    public void expect_group_is_loaded() throws IOException {
        final List<URL> files = collectFiles("/hello_world/repo-config.yaml");
        final ConfigurationParser parser = new ConfigurationParser();
        final Map<String, Configuration> configurations = parser.parse(files);

        assertEquals(2, configurations.size());

        final Configuration base = configurations.get("base");
        assertEquals("base", base.getName());
        assertEquals(0, base.getDependsOn().size());
        assertEquals(1, base.getProjects().size());
        final Project project1 = base.getProjects().get("project1");
        assertEquals("project1", project1.getName());
        assertEquals(1, project1.getModules().size());
        final Module module1 = project1.getModules().get("module1");
        assertEquals("module1", project1.getName());

        final Configuration myhippoproject = configurations.get("myhippoproject");
        assertEquals("myhippoproject", myhippoproject.getName());
        assertEquals(1, myhippoproject.getDependsOn().size());
        assertEquals("base/myhippoproject", myhippoproject.getDependsOn().get(0));
        assertEquals(1, myhippoproject.getProjects().size());
        final Project project2 = myhippoproject.getProjects().get("project2");
        assertEquals("project2", project1.getName());
        assertEquals(1, project2.getModules().size());
        final Module module2 = project2.getModules().get("module2");
        assertEquals("module2", project1.getName());
    }

}
