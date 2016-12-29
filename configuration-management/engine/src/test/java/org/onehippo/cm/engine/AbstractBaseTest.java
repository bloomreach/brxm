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
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionItem;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.ValueFormatException;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class AbstractBaseTest {

    private URL pathToUrl(final Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            fail("Cannot convert path to URL" + e);
            return null;
        }
    }

    static class TestFiles {
        URL repoConfig;
        List<URL> sources;
    }

    TestFiles collectFiles(final String repoConfig) throws IOException {
        TestFiles testFiles = new TestFiles();
        testFiles.repoConfig = ConfigurationParserTest.class.getResource(repoConfig);
        if (testFiles.repoConfig == null) {
            fail("cannot find resource " + repoConfig);
        }
        final String fileName = testFiles.repoConfig.getFile();
        testFiles.sources = new ArrayList<>();
        final Path testRootDirectory = Paths.get(fileName.substring(0, fileName.lastIndexOf(".")));
        Files.find(testRootDirectory, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                .forEachOrdered(path -> testFiles.sources.add(pathToUrl(path)));
        return testFiles;
    }

    Configuration assertConfiguration(final Map<String, Configuration> parent, final String name, final String[] after, final int projectCount) {
        final Configuration configuration = parent.get(name);
        assertNotNull(configuration);
        assertEquals(name, configuration.getName());
        assertArrayEquals(after, configuration.getAfter().toArray());
        assertEquals(projectCount, configuration.getProjects().size());
        return configuration;
    }

    Project assertProject(final Configuration parent, final String name, final String[] after, final int moduleCount) {
        final Project project = parent.getProjects().get(name);
        assertNotNull(project);
        assertEquals(name, project.getName());
        assertArrayEquals(after, project.getAfter().toArray());
        assertEquals(parent, project.getConfiguration());
        assertEquals(moduleCount, project.getModules().size());
        return project;
    }

    Module assertModule(final Project parent, final String name, final String[] after, final int sourceCount) {
        final Module module = parent.getModules().get(name);
        assertNotNull(module);
        assertEquals(name, module.getName());
        assertArrayEquals(after, module.getAfter().toArray());
        assertEquals(parent, module.getProject());
        assertEquals(sourceCount, module.getSources().size());
        return module;
    }

    Source assertSource(final Module parent, final String nameSuffix, final int definitionCount) {
        Source source = null;
        for (String name : parent.getSources().keySet()) {
            if (name.endsWith(nameSuffix)) {
                if (source == null) {
                    source = parent.getSources().get(name); // don't break but keep looking to ensure suffix is unique
                } else {
                    fail("Multiple sources found with suffix '" + nameSuffix + "'");
                }
            }
        }
        assertNotNull(source);
        assertThat(source.getPath(), endsWith(nameSuffix));
        assertEquals(parent, source.getModule());
        assertEquals(definitionCount, source.getDefinitions().size());
        return source;
    }

    <T extends Definition> T assertDefinition(final Source parent, final int index, Class<T> definitionClass) {
        final Definition definition = parent.getDefinitions().get(index);
        return definitionClass.cast(definition);
    }

    DefinitionNode assertNode(final DefinitionNode parent,
                              final String name,
                              final String path,
                              final boolean isRoot,
                              final Definition definition,
                              final boolean isDeleted,
                              final int nodeCount,
                              final int propertyCount)
    {
        final DefinitionNode node = parent.getNodes().get(name);
        validateNode(node, name, path, parent, isRoot, definition, isDeleted, nodeCount, propertyCount);
        return node;
    }

    DefinitionNode assertNode(final ConfigDefinition parent,
                              final String name,
                              final String path,
                              final Definition definition,
                              final boolean isDeleted,
                              final int nodeCount,
                              final int propertyCount)
    {
        final DefinitionNode node = parent.getNode();
        validateNode(node, name, path, null, true, definition, isDeleted, nodeCount, propertyCount);
        return node;
    }

    private void validateNode(final DefinitionNode node,
                              final String name,
                              final String path,
                              final DefinitionNode parent,
                              final boolean isRoot,
                              final Definition definition,
                              final boolean isDeleted,
                              final int nodeCount,
                              final int propertyCount)
    {
        validateItem(node, name, path, parent, isRoot, definition, isDeleted);
        assertEquals(nodeCount, node.getNodes().size());
        assertEquals(propertyCount, node.getProperties().size());
    }

    private void validateItem(final DefinitionItem item,
                              final String name,
                              final String path,
                              final DefinitionNode parent,
                              final boolean isRoot,
                              final Definition definition,
                              final boolean isDeleted)
    {
        assertNotNull(item);
        assertEquals(name, item.getName());
        assertEquals(path, item.getPath());
        if (isRoot) {
            try {
                item.getParent();
                fail("Expected IllegalStateException");
            } catch (IllegalStateException e) {
                // ignore
            }
        } else {
            assertEquals(parent, item.getParent());
        }
        assertEquals(isRoot, item.isRoot());
        assertEquals(definition, item.getDefinition());
        assertEquals(isDeleted, item.isDeleted());
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String name,
                                      final String path,
                                      final Definition definition,
                                      final boolean isDeleted,
                                      final Object value)
    {
        final DefinitionProperty property = parent.getProperties().get(name);
        validateItem(property, name, path, parent, false, definition, isDeleted);
        try {
            property.getValues();
            fail("Expected ValueFormatException");
        } catch (ValueFormatException e) {
            // ignore
        }
        if (value instanceof byte[]) {
            assertArrayEquals((byte[]) value, (byte[]) property.getValue().getObject());
        } else {
            assertEquals(value, property.getValue().getObject());
        }
        return property;
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String name,
                                      final String path,
                                      final Definition definition,
                                      final boolean isDeleted,
                                      final Object[] values)
    {
        final DefinitionProperty property = parent.getProperties().get(name);
        validateItem(property, name, path, parent, false, definition, isDeleted);
        try {
            property.getValue();
            fail("Expected ValueFormatException");
        } catch (ValueFormatException e) {
            // ignore
        }
        assertEquals(values.length, property.getValues().length);
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], property.getValues()[i].getString());
        }
        return property;
    }

}
