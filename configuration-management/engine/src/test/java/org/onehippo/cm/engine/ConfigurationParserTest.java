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
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigurationGroup;
import org.onehippo.cm.api.model.ConfigurationModule;

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

    @Test
    public void expect_group_is_loaded() throws IOException {
        final List<URL> files = collectFiles("/hello_world/group2/module2/module-config.yaml");
        final ConfigurationParser parser = new ConfigurationParser();
        final List<ConfigurationGroup> groups = parser.parse(files);

        assertEquals(2, groups.size());
        final ConfigurationGroup group1 = groups.get(0);
        assertEquals("group1", group1.getName());
        assertEquals(null, group1.getDependsOn());
        assertEquals(1, group1.getModules().size());
        final ConfigurationModule module1 = group1.getModules().get(0);
        assertEquals("module1", module1.getName());
        assertEquals(group1, module1.getGroup());
        assertEquals(Collections.emptyList(), module1.getDependsOn());
        assertEquals(Collections.emptyList(), module1.getSources());

        final ConfigurationGroup group2 = groups.get(1);
        assertEquals("group2", group2.getName());
        assertEquals(null, group2.getDependsOn());
        assertEquals(1, group2.getModules().size());
        final ConfigurationModule module2 = group2.getModules().get(0);
        assertEquals("module2", module2.getName());
        assertEquals(group2, module2.getGroup());
        final List<ConfigurationModule> dependency = new ArrayList<>();
        dependency.add(module1);
        assertEquals(dependency, module2.getDependsOn());
        assertEquals(Collections.emptyList(), module2.getSources());
    }

    /** TODO:
     * create tests for:
     * - validation that groups do not create circular dependency
     * - module with multiple dependencies
     * discuss & create tests for:
     * - must the module name be unique across groups?
     */
}
