/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.TreeDefinitionImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionItemImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.parser.PathConfigurationReader;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.tree.PropertyOperation;
import org.onehippo.cm.model.tree.ValueFormatException;
import org.onehippo.cm.model.tree.ValueType;
import org.onehippo.cm.model.util.FilePathUtils;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.onehippo.cm.model.Constants.DEFAULT_EXPLICIT_SEQUENCING;
import static org.onehippo.cm.model.impl.ModelTestUtils.findByName;
import static org.onehippo.cm.model.impl.ModelTestUtils.findByPath;
import static org.onehippo.cm.model.util.FilePathUtils.getParentOrFsRoot;

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

    };

    protected PathConfigurationReader.ReadResult readFromResource(final String resourceName) throws IOException, ParserException {
        return readFromResource(resourceName, DEFAULT_EXPLICIT_SEQUENCING);
    }

    protected PathConfigurationReader.ReadResult readFromResource(final String resourceName, final boolean explicitSequencing) throws IOException, ParserException {
        final Path moduleConfig = find(resourceName);
        return new PathConfigurationReader(explicitSequencing).read(moduleConfig);
    }

    protected PathConfigurationReader.ReadResult readFromTestJar(final String resourceName) throws IOException, ParserException {
        final Path jarPath = Paths.get("target/test-classes.jar");
        try (FileSystem fs = FileSystems.newFileSystem(jarPath, null)) {
            final Path moduleConfig = fs.getPath(resourceName);
            return new PathConfigurationReader(DEFAULT_EXPLICIT_SEQUENCING).read(moduleConfig);
        }
    }

    protected Path find(final String moduleConfigResourceName) throws IOException {
        final URL url = AbstractBaseTest.class.getResource(moduleConfigResourceName);
        if (url == null) {
            fail("cannot find resource " + moduleConfigResourceName);
        }
        return Paths.get(FilePathUtils.getNativeFilePath(url));
    }

    protected Path findBase(final String moduleConfigResourceName) throws IOException {
        return getParentOrFsRoot(find(moduleConfigResourceName));
    }

    protected GroupImpl assertGroup(final Map<String, GroupImpl> parent, final String name, final String[] after, final int projectCount) {
        final GroupImpl group = parent.get(name);
        assertNotNull(group);
        assertEquals(name, group.getName());
        assertArrayEquals(after, group.getAfter().toArray());
        assertEquals(projectCount, group.getProjects().size());
        return group;
    }

    protected ProjectImpl assertProject(final GroupImpl parent, final String name, final String[] after, final int moduleCount) {
        final ProjectImpl project = findByName(name, parent.getProjects());
        assertNotNull(project);
        assertEquals(name, project.getName());
        assertArrayEquals(after, project.getAfter().toArray());
        assertEquals(parent, project.getGroup());
        assertEquals(moduleCount, project.getModules().size());
        return project;
    }

    protected ModuleImpl assertModule(final ProjectImpl parent, final String name, final String[] after, final int sourceCount) {
        final ModuleImpl module = findByName(name, parent.getModules());
        assertNotNull(module);
        assertEquals(name, module.getName());
        assertArrayEquals(after, module.getAfter().toArray());
        assertEquals(parent, module.getProject());
        assertEquals(sourceCount, module.getSources().size());
        return module;
    }

    public static SourceImpl assertSource(final ModuleImpl parent, final String path, final int definitionCount) {
        SourceImpl source = findByPath(path, parent.getSources());
        assertNotNull(source);
        assertEquals(path, source.getPath());
        assertEquals(parent, source.getModule());
        assertEquals(definitionCount, source.getDefinitions().size());
        return source;
    }

    protected <T extends AbstractDefinitionImpl> T assertDefinition(final SourceImpl parent, final int index, Class<T> definitionClass) {
        final AbstractDefinitionImpl definition = parent.getDefinitions().get(index);
        return definitionClass.cast(definition);
    }

    protected DefinitionNodeImpl assertNode(final DefinitionNodeImpl parent,
                                            final String path,
                                            final String name,
                                            final AbstractDefinitionImpl definition,
                                            final int nodeCount,
                                            final int propertyCount)
    {
        return assertNode(parent, path, name, definition, false, null, nodeCount, propertyCount);
    }

    protected DefinitionNodeImpl assertNode(final DefinitionNodeImpl parent,
                              final String path,
                              final String name,
                              final AbstractDefinitionImpl definition,
                              final boolean isDelete,
                              final String orderBefore,
                              final int nodeCount,
                              final int propertyCount)
    {
        final DefinitionNodeImpl node = parent.getNode(name);
        validateNode(node, path, name, parent, false, definition, isDelete, orderBefore, nodeCount, propertyCount);
        return node;
    }

    protected DefinitionNodeImpl assertNode(final TreeDefinitionImpl parent,
                              final String path,
                              final String name,
                              final AbstractDefinitionImpl definition,
                              final int nodeCount,
                              final int propertyCount)
    {
        return assertNode(parent, path, name, definition, false, null, nodeCount, propertyCount);
    }

    protected DefinitionNodeImpl assertNode(final TreeDefinitionImpl parent,
                              final String path,
                              final String name,
                              final AbstractDefinitionImpl definition,
                              final boolean isDelete,
                              final String orderBefore,
                              final int nodeCount,
                              final int propertyCount)
    {
        final DefinitionNodeImpl node = parent.getNode();
        validateNode(node, path, name, null, true, definition, isDelete, orderBefore, nodeCount, propertyCount);
        return node;
    }

    private void validateNode(final DefinitionNodeImpl node,
                              final String path,
                              final String name,
                              final DefinitionNodeImpl parent,
                              final boolean isRoot,
                              final AbstractDefinitionImpl definition,
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

    private void validateItem(final DefinitionItemImpl item,
                              final String path,
                              final String name,
                              final DefinitionNodeImpl parent,
                              final boolean isRoot,
                              final AbstractDefinitionImpl definition)
    {
        assertNotNull(item);
        assertEquals(path, item.getJcrPath().toString());
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

    protected DefinitionPropertyImpl assertProperty(final DefinitionNodeImpl parent,
                                                    final String path,
                                                    final String name,
                                                    final AbstractDefinitionImpl definition,
                                                    final ValueType valueType,
                                                    final Object value)
    {
        return assertProperty(parent, path, name, definition, PropertyOperation.REPLACE, valueType, value, false, false);
    }

    protected DefinitionPropertyImpl assertProperty(final DefinitionNodeImpl parent,
                                      final String path,
                                      final String name,
                                      final AbstractDefinitionImpl definition,
                                      final PropertyOperation operation,
                                      final ValueType valueType,
                                      final Object value)
    {
        return assertProperty(parent, path, name, definition, operation, valueType, value, false, false);
    }

    protected DefinitionPropertyImpl assertProperty(final DefinitionNodeImpl parent,
                                      final String path,
                                      final String name,
                                      final AbstractDefinitionImpl definition,
                                      final PropertyOperation operation,
                                      final ValueType valueType,
                                      final Object value,
                                      final boolean valueIsResource,
                                      final boolean valueIsPath)
    {
        final DefinitionPropertyImpl property = parent.getProperty(name);
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

    protected DefinitionPropertyImpl assertProperty(final DefinitionNodeImpl parent,
                                      final String path,
                                      final String name,
                                      final AbstractDefinitionImpl definition,
                                      final ValueType valueType,
                                      final Object[] values)
    {
        return assertProperty(parent, path, name, definition, PropertyOperation.REPLACE, valueType, values, false, false);
    }

    protected DefinitionPropertyImpl assertProperty(final DefinitionNodeImpl parent,
                                      final String path,
                                      final String name,
                                      final AbstractDefinitionImpl definition,
                                      final PropertyOperation operation,
                                      final ValueType valueType,
                                      final Object[] values)
    {
        return assertProperty(parent, path, name, definition, operation, valueType, values, false, false);
    }

    protected DefinitionPropertyImpl assertProperty(final DefinitionNodeImpl parent,
                                      final String path,
                                      final String name,
                                      final AbstractDefinitionImpl definition,
                                      final PropertyOperation operation,
                                      final ValueType valueType,
                                      final Object[] values,
                                      final boolean valuesAreResource,
                                      final boolean valuesArePath)
    {
        final DefinitionPropertyImpl property = parent.getProperty(name);
        validateItem(property, path, name, parent, false, definition);
        assertEquals(operation, property.getOperation());
        assertEquals(valueType, property.getValueType());
        try {
            property.getValue();
            fail("Expected ValueFormatException");
        } catch (ValueFormatException e) {
            // ignore
        }
        assertEquals(values.length, property.getValues().size());
        for (int i = 0; i < values.length; i++) {
            assertValue(valueType, values[i], valuesAreResource, valuesArePath, property, property.getValues().get(i));
        }
        return property;
    }

    protected void assertValue(final ValueType valueType, final Object expected, final boolean isResource, final boolean isPath,
                               final DefinitionPropertyImpl parent, final ValueImpl actual) {
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


    protected void assertNoFileDiff(final Path expectedRoot, final Path actualRoot) throws IOException {
        assertNoFileDiff(null, expectedRoot, actualRoot);
    }

    protected void assertNoFileDiff(final String msg, final Path expectedRoot, final Path actualRoot) throws IOException {
        final List<Path> expected = findFiles(expectedRoot);
        final List<Path> actual = findFiles(actualRoot);

        assertEquals(msg, expected.stream().map(Path::toString).collect(toList()),
                actual.stream().map(Path::toString).collect(toList()));
        for (int i = 0; i < expected.size(); i++) {
            assertEquals("comparing "+expected.get(i),
                    new String(Files.readAllBytes(expectedRoot.resolve(expected.get(i)))).trim(),
                    new String(Files.readAllBytes(actualRoot.resolve(actual.get(i)))).trim());
        }
    }

    private List<Path> findFiles(final Path root) throws IOException {
        final List<Path> paths = new ArrayList<>();
        //Ignore hcm-actions file as it is not being serialized to disk
        final BiPredicate<Path, BasicFileAttributes> matcher = (filePath, fileAttr) -> fileAttr.isRegularFile() && !filePath.endsWith(Constants.ACTIONS_YAML);
        Files.find(root, Integer.MAX_VALUE, matcher).forEachOrdered((path) -> paths.add(root.relativize(path)));
        Collections.sort(paths);
        return paths;
    }
}
