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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionItem;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueFormatException;
import org.onehippo.cm.api.model.ValueType;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public abstract class AbstractBaseTest {

    protected static final ResourceInputProvider DUMMY_RESOURCE_INPUT_PROVIDER = new ResourceInputProvider() {
        @Override
        public boolean hasResource(final Source source, final String resourcePath) {
            return true;
        }

        @Override
        public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
            throw new UnsupportedOperationException();
        }
    };

    protected FileConfigurationReader.ReadResult readFromResource(final String resourceName) throws IOException, ParserException {
        final Path repoConfig = find(resourceName);
        return new FileConfigurationReader().read(repoConfig);
    }

    Path find(final String repoConfigResourceName) throws IOException {
        final URL url = AbstractBaseTest.class.getResource(repoConfigResourceName);
        if (url == null) {
            fail("cannot find resource " + repoConfigResourceName);
        }
        return Paths.get(url.getFile());
    }

    Path findBase(final String repoConfigResourceName) throws IOException {
        return find(repoConfigResourceName).getParent();
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

    Source assertSource(final Module parent, final String path, final int definitionCount) {
        Source source = parent.getSources().get(path);
        assertNotNull(source);
        assertEquals(path, source.getPath());
        assertEquals(parent, source.getModule());
        assertEquals(definitionCount, source.getDefinitions().size());
        return source;
    }

    <T extends Definition> T assertDefinition(final Source parent, final int index, Class<T> definitionClass) {
        final Definition definition = parent.getDefinitions().get(index);
        return definitionClass.cast(definition);
    }

    DefinitionNode assertNode(final DefinitionNode parent,
                              final String path,
                              final String name,
                              final boolean isRoot,
                              final Definition definition,
                              final boolean isDeleted,
                              final int nodeCount,
                              final int propertyCount)
    {
        final DefinitionNode node = parent.getNodes().get(name);
        validateNode(node, path, name, parent, isRoot, definition, isDeleted, nodeCount, propertyCount);
        return node;
    }

    DefinitionNode assertNode(final ContentDefinition parent,
                              final String path,
                              final String name,
                              final Definition definition,
                              final boolean isDeleted,
                              final int nodeCount,
                              final int propertyCount)
    {
        final DefinitionNode node = parent.getNode();
        validateNode(node, path, name, null, true, definition, isDeleted, nodeCount, propertyCount);
        return node;
    }

    private void validateNode(final DefinitionNode node,
                              final String path,
                              final String name,
                              final DefinitionNode parent,
                              final boolean isRoot,
                              final Definition definition,
                              final boolean isDeleted,
                              final int nodeCount,
                              final int propertyCount)
    {
        validateItem(node, path, name, parent, isRoot, definition, isDeleted);
        assertEquals(nodeCount, node.getNodes().size());
        assertEquals(propertyCount, node.getProperties().size());
    }

    private void validateItem(final DefinitionItem item,
                              final String path,
                              final String name,
                              final DefinitionNode parent,
                              final boolean isRoot,
                              final Definition definition,
                              final boolean isDeleted)
    {
        assertNotNull(item);
        assertEquals(path, item.getPath());
        assertEquals(name, item.getName());
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
                                      final String path,
                                      final String name,
                                      final Definition definition,
                                      final ValueType valueType,
                                      final Object value)
    {
        return assertProperty(parent, path, name, definition, false, valueType, value, false, false);
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String path,
                                      final String name,
                                      final Definition definition,
                                      final boolean isDeleted,
                                      final ValueType valueType,
                                      final Object value,
                                      final boolean valueIsResource,
                                      final boolean valueIsPath)
    {
        final DefinitionProperty property = parent.getProperties().get(name);
        validateItem(property, path, name, parent, false, definition, isDeleted);
        try {
            property.getValues();
            fail("Expected ValueFormatException");
        } catch (ValueFormatException e) {
            // ignore
        }
        assertValue(valueType, value, valueIsResource, valueIsPath, property.getValue());
        return property;
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String path,
                                      final String name,
                                      final Definition definition,
                                      final ValueType valueType,
                                      final Object[] values)
    {
        return assertProperty(parent, path, name, definition, false, valueType, values, false, false);
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String path,
                                      final String name,
                                      final Definition definition,
                                      final boolean isDeleted,
                                      final ValueType valueType,
                                      final Object[] values,
                                      final boolean valuesAreResource,
                                      final boolean valuesArePath)
    {
        final DefinitionProperty property = parent.getProperties().get(name);
        validateItem(property, path, name, parent, false, definition, isDeleted);
        assertEquals(valueType, property.getValueType());
        try {
            property.getValue();
            fail("Expected ValueFormatException");
        } catch (ValueFormatException e) {
            // ignore
        }
        assertEquals(values.length, property.getValues().length);
        for (int i = 0; i < values.length; i++) {
            assertValue(valueType, values[i], valuesAreResource, valuesArePath, property.getValues()[i]);
        }
        return property;
    }

    void assertValue(final ValueType valueType, final Object expected, final boolean isResource, final boolean isPath, final Value actual) {
        if (expected instanceof byte[]) {
            assertArrayEquals((byte[]) expected, (byte[]) actual.getObject());
        } else {
            assertEquals(expected, actual.getObject());
        }
        assertEquals(valueType, actual.getType());
        assertEquals(isResource, actual.isResource());
        assertEquals(isPath, actual.isPath());
    }

}
