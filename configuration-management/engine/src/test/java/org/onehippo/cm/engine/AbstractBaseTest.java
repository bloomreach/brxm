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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Group;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionItem;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.PropertyOperation;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueFormatException;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.engine.parser.ParserException;
import org.onehippo.cm.impl.model.GroupImpl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.onehippo.cm.engine.Constants.DEFAULT_EXPLICIT_SEQUENCING;
import static org.onehippo.cm.impl.model.ModelTestUtils.findByName;
import static org.onehippo.cm.impl.model.ModelTestUtils.findByPath;

public abstract class AbstractBaseTest {

    protected static final ResourceInputProvider DUMMY_RESOURCE_INPUT_PROVIDER = new ResourceInputProvider() {
        @Override
        public boolean hasResource(final Source source, final String resourcePath) {
            return resourcePath.equals("resource.txt");
        }
        @Override
        public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public URL getBaseURL() {
            throw new UnsupportedOperationException();
        }
    };

    protected PathConfigurationReader.ReadResult readFromResource(final String resourceName) throws IOException, ParserException {
        return readFromResource(resourceName, DEFAULT_EXPLICIT_SEQUENCING);
    }

    protected PathConfigurationReader.ReadResult readFromResource(final String resourceName, final boolean explicitSequencing) throws IOException, ParserException {
        final Path moduleConfig = find(resourceName);
        return new PathConfigurationReader(explicitSequencing).read(moduleConfig);
    }

    protected PathConfigurationReader.ReadResult readFromTestJar(final String resourceName) throws IOException, ParserException {
        final Path jarPath = new File("target/test-classes.jar").toPath();
        try (FileSystem fs = FileSystems.newFileSystem(jarPath, null)) {
            final Path moduleConfig = fs.getPath(resourceName);
            return new PathConfigurationReader(DEFAULT_EXPLICIT_SEQUENCING).read(moduleConfig);
        }
    }

    Path find(final String moduleConfigResourceName) throws IOException {
        final URL url = AbstractBaseTest.class.getResource(moduleConfigResourceName);
        if (url == null) {
            fail("cannot find resource " + moduleConfigResourceName);
        }
        return Paths.get(url.getFile());
    }

    Path findBase(final String moduleConfigResourceName) throws IOException {
        return find(moduleConfigResourceName).getParent();
    }

    Group assertConfiguration(final Map<String, GroupImpl> parent, final String name, final String[] after, final int projectCount) {
        final Group group = parent.get(name);
        assertNotNull(group);
        assertEquals(name, group.getName());
        assertArrayEquals(after, group.getAfter().toArray());
        assertEquals(projectCount, group.getProjects().size());
        return group;
    }

    Project assertProject(final Group parent, final String name, final String[] after, final int moduleCount) {
        final Project project = findByName(name, parent.getProjects());
        assertNotNull(project);
        assertEquals(name, project.getName());
        assertArrayEquals(after, project.getAfter().toArray());
        assertEquals(parent, project.getGroup());
        assertEquals(moduleCount, project.getModules().size());
        return project;
    }

    Module assertModule(final Project parent, final String name, final String[] after, final int sourceCount) {
        final Module module = findByName(name, parent.getModules());
        assertNotNull(module);
        assertEquals(name, module.getName());
        assertArrayEquals(after, module.getAfter().toArray());
        assertEquals(parent, module.getProject());
        assertEquals(sourceCount, module.getSources().size());
        return module;
    }

    Source assertSource(final Module parent, final String path, final int definitionCount) {
        Source source = findByPath(path, parent.getSources());
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
                              final Definition definition,
                              final int nodeCount,
                              final int propertyCount)
    {
        return assertNode(parent, path, name, definition, false, null, nodeCount, propertyCount);
    }

    DefinitionNode assertNode(final DefinitionNode parent,
                              final String path,
                              final String name,
                              final Definition definition,
                              final boolean isDelete,
                              final String orderBefore,
                              final int nodeCount,
                              final int propertyCount)
    {
        final DefinitionNode node = parent.getNodes().get(name);
        validateNode(node, path, name, parent, false, definition, isDelete, orderBefore, nodeCount, propertyCount);
        return node;
    }

    DefinitionNode assertNode(final ContentDefinition parent,
                              final String path,
                              final String name,
                              final Definition definition,
                              final int nodeCount,
                              final int propertyCount)
    {
        return assertNode(parent, path, name, definition, false, null, nodeCount, propertyCount);
    }

    DefinitionNode assertNode(final ContentDefinition parent,
                              final String path,
                              final String name,
                              final Definition definition,
                              final boolean isDelete,
                              final String orderBefore,
                              final int nodeCount,
                              final int propertyCount)
    {
        final DefinitionNode node = parent.getNode();
        validateNode(node, path, name, null, true, definition, isDelete, orderBefore, nodeCount, propertyCount);
        return node;
    }

    private void validateNode(final DefinitionNode node,
                              final String path,
                              final String name,
                              final DefinitionNode parent,
                              final boolean isRoot,
                              final Definition definition,
                              final boolean isDelete,
                              final String orderBefore,
                              final int nodeCount,
                              final int propertyCount)
    {
        validateItem(node, path, name, parent, isRoot, definition);
        assertEquals(isDelete, node.isDelete());
        if (orderBefore == null) {
            assertNull(node.getOrderBefore());
        } else {
            assertEquals(orderBefore, node.getOrderBefore());
        }
        assertEquals(nodeCount, node.getNodes().size());
        assertEquals(propertyCount, node.getProperties().size());
    }

    private void validateItem(final DefinitionItem item,
                              final String path,
                              final String name,
                              final DefinitionNode parent,
                              final boolean isRoot,
                              final Definition definition)
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
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String path,
                                      final String name,
                                      final Definition definition,
                                      final ValueType valueType,
                                      final Object value)
    {
        return assertProperty(parent, path, name, definition, PropertyOperation.REPLACE, valueType, value, false, false);
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String path,
                                      final String name,
                                      final Definition definition,
                                      final PropertyOperation operation,
                                      final ValueType valueType,
                                      final Object value)
    {
        return assertProperty(parent, path, name, definition, operation, valueType, value, false, false);
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String path,
                                      final String name,
                                      final Definition definition,
                                      final PropertyOperation operation,
                                      final ValueType valueType,
                                      final Object value,
                                      final boolean valueIsResource,
                                      final boolean valueIsPath)
    {
        final DefinitionProperty property = parent.getProperties().get(name);
        validateItem(property, path, name, parent, false, definition);
        assertEquals(operation, property.getOperation());
        try {
            property.getValues();
            fail("Expected ValueFormatException");
        } catch (ValueFormatException e) {
            // ignore
        }
        assertValue(valueType, value, valueIsResource, valueIsPath, property, property.getValue());
        return property;
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String path,
                                      final String name,
                                      final Definition definition,
                                      final ValueType valueType,
                                      final Object[] values)
    {
        return assertProperty(parent, path, name, definition, PropertyOperation.REPLACE, valueType, values, false, false);
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String path,
                                      final String name,
                                      final Definition definition,
                                      final PropertyOperation operation,
                                      final ValueType valueType,
                                      final Object[] values)
    {
        return assertProperty(parent, path, name, definition, operation, valueType, values, false, false);
    }

    DefinitionProperty assertProperty(final DefinitionNode parent,
                                      final String path,
                                      final String name,
                                      final Definition definition,
                                      final PropertyOperation operation,
                                      final ValueType valueType,
                                      final Object[] values,
                                      final boolean valuesAreResource,
                                      final boolean valuesArePath)
    {
        final DefinitionProperty property = parent.getProperties().get(name);
        validateItem(property, path, name, parent, false, definition);
        assertEquals(operation, property.getOperation());
        assertEquals(valueType, property.getValueType());
        try {
            property.getValue();
            fail("Expected ValueFormatException");
        } catch (ValueFormatException e) {
            // ignore
        }
        assertEquals(values.length, property.getValues().length);
        for (int i = 0; i < values.length; i++) {
            assertValue(valueType, values[i], valuesAreResource, valuesArePath, property, property.getValues()[i]);
        }
        return property;
    }

    void assertValue(final ValueType valueType, final Object expected, final boolean isResource, final boolean isPath,
                     final DefinitionProperty parent, final Value actual) {
        if (expected instanceof byte[]) {
            assertArrayEquals((byte[]) expected, (byte[]) actual.getObject());
        } else {
            assertEquals(expected, actual.getObject());
        }
        assertEquals(valueType, actual.getType());
        assertEquals(isResource, actual.isResource());
        assertEquals(isPath, actual.isPath());
        assertEquals(parent, actual.getParent());
    }
}
