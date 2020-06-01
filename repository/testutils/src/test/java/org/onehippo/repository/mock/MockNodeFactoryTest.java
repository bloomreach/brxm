/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.onehippo.cm.model.path.JcrPaths;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MockNodeFactoryTest {

    @Test(expected = IOException.class)
    public void xmlResourceDoesNotExist() throws RepositoryException, JAXBException, IOException {
        MockNodeFactory.fromXml("/no/such/resource.xml");
    }

    @Test(expected = IOException.class)
    public void yamlResourceDoesNotExist() throws RepositoryException, IOException {
        MockNodeFactory.fromYaml("/no/such/resource.yaml");
    }

    @Test
    public void xmlEmptyNode() throws JAXBException, IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromXml(getClass().getResource("MockNodeFactoryTest-empty-node.xml"));
        assertEquals("", root.getName());
        assertEquals("/", root.getPath());
        assertEquals("nt:unstructured", root.getPrimaryNodeType().getName());
    }

    @Test
    public void yamlEmptyNode() throws IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromYaml(getClass().getResource("MockNodeFactoryTest-empty-node.yaml"));
        final MockNode node = root.getNode("empty");
        assertEquals("empty", node.getName());
        assertEquals("/empty", node.getPath());
        assertEquals("nt:base", node.getPrimaryNodeType().getName());
    }

    @Test
    public void xmlStringProperty() throws JAXBException, IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.xml");
        assertStringProperty(root);
    }

    @Test
    public void yamlStringProperty() throws IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.yaml");
        assertStringProperty(root.getNode("nodeWithProperties"));
    }

    private void assertStringProperty(final MockNode node) throws RepositoryException {
        Property property = node.getProperty("stringProperty");
        assertFalse(property.isMultiple());
        assertEquals("aaa", property.getString());
        assertPathBelow(node, "stringProperty", property.getPath());
    }

    @Test
    public void xmlStringPropertyNotMultiple() throws JAXBException, IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.xml");
        assertStringPropertyNotMultiple(root);
    }

    @Test
    public void yamlStringPropertyNotMultiple() throws JAXBException, IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.yaml");
        assertStringPropertyNotMultiple(root.getNode("nodeWithProperties"));
    }

    private void assertStringPropertyNotMultiple(final MockNode node) throws RepositoryException {
        Property property = node.getProperty("stringPropertyNotMultiple");
        assertFalse(property.isMultiple());
        assertPathBelow(node, "stringPropertyNotMultiple", property.getPath());
        assertEquals("bbb", property.getString());
    }

    @Test
    public void xmlMultipleStringProperty() throws JAXBException, IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.xml");
        assertMultipleStringProperty(root);
    }

    @Test
    public void yamlMultipleStringProperty() throws JAXBException, IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.yaml");
        assertMultipleStringProperty(root.getNode("nodeWithProperties"));
    }

    private void assertMultipleStringProperty(final MockNode node) throws RepositoryException {
        Property property = node.getProperty("multipleStringProperty");
        assertTrue(property.isMultiple());
        assertPathBelow(node, "multipleStringProperty", property.getPath());

        Value[] expected = new MockValue[2];
        expected[0] = new MockValue(PropertyType.STRING, "ccc");
        expected[1] = new MockValue(PropertyType.STRING, "ddd");
        assertArrayEquals(expected, property.getValues());
    }

    @Test
    public void xmlMultipleStringPropertyWithOneValue() throws JAXBException, IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.xml");
        assertMultipleStringPropertyWithOneValue(root);
    }

    @Test
    public void yamlMultipleStringPropertyWithOneValue() throws IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.yaml");
        assertMultipleStringPropertyWithOneValue(root.getNode("nodeWithProperties"));
    }

    private void assertMultipleStringPropertyWithOneValue(final MockNode node) throws RepositoryException {
        Property property = node.getProperty("multipleStringPropertyWithOneValue");
        assertTrue(property.isMultiple());
        assertPathBelow(node, "multipleStringPropertyWithOneValue", property.getPath());

        Value[] expected = new MockValue[1];
        expected[0] = new MockValue(PropertyType.STRING, "eee");
        assertArrayEquals(expected, property.getValues());
    }

    @Test
    public void xmlNodeWithChild() throws JAXBException, IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-child.xml");
        assertNodeWithChild(root);
    }

    @Test
    public void yamlNodeWithChild() throws IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-child.yaml");
        assertNodeWithChild(root.getNode("nodeWithChild"));
    }

    private void assertNodeWithChild(final MockNode node) throws RepositoryException {
        Property parentProperty = node.getProperty("parentProperty");
        assertFalse(parentProperty.isMultiple());
        assertPathBelow(node, "parentProperty", parentProperty.getPath());
        assertEquals("parentValue", parentProperty.getValue().getString());

        assertTrue(node.hasNode("child"));

        Node child = node.getNode("child");
        assertEquals("child", child.getName());
        assertPathBelow(node, "child", child.getPath());
        assertEquals(node, child.getParent());
        assertEquals("10065434-57ea-4153-8165-e7a22288c05d", child.getIdentifier());
        assertTrue(child.hasProperties());
        assertTrue(child.hasProperty("childProperty"));

        Property childProperty = child.getProperty("childProperty");
        assertFalse(childProperty.isMultiple());
        assertPathBelow(node, "child/childProperty", childProperty.getPath());
        assertEquals("childValue", childProperty.getString());
    }

    @Test
    public void xmlNodeWithChildren() throws JAXBException, IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-children.xml");
        assertNodeWithChildren(root);
    }

    @Test
    public void yamlNodeWithChildren() throws IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-children.yaml");
        assertNodeWithChildren(root.getNode("nodeWithChildren"));
    }

    private void assertNodeWithChildren(final MockNode node) throws RepositoryException {
        assertTrue(node.hasNode("child1"));
        assertTrue(node.hasNode("child2"));

        Node firstChild = node.getNode("child1");
        assertEquals("child1", firstChild.getName());
        assertPathBelow(node, "child1", firstChild.getPath());
        assertEquals(node, firstChild.getParent());
        assertTrue(firstChild.hasProperties());
        assertTrue(firstChild.hasProperty("childProperty"));

        Property firstChildProperty = firstChild.getProperty("childProperty");
        assertFalse(firstChildProperty.isMultiple());
        assertPathBelow(node, "child1/childProperty", firstChildProperty.getPath());
        assertEquals("value1", firstChildProperty.getString());

        Node secondChild = node.getNode("child2");
        assertEquals("child2", secondChild.getName());
        assertPathBelow(node, "child2", secondChild.getPath());
        assertEquals(node, secondChild.getParent());
        assertTrue(secondChild.hasProperties());
        assertTrue(secondChild.hasProperty("childProperty"));

        Property secondChildProperty = secondChild.getProperty("childProperty");
        assertFalse(secondChildProperty.isMultiple());
        assertPathBelow(node, "child2/childProperty", secondChildProperty.getPath());
        assertEquals("value2", secondChildProperty.getString());
    }

    @Test
    public void xmlNodeIteratorReturnsAllChildren() throws JAXBException, IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-children.xml");
        assertNodeIteratorReturnsAllChildren(root);
    }

    @Test
    public void yamlNodeIteratorReturnsAllChildren() throws IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-children.yaml");
        assertNodeIteratorReturnsAllChildren(root.getNode("nodeWithChildren"));
    }

    private void assertNodeIteratorReturnsAllChildren(final MockNode root) throws RepositoryException {
        NodeIterator iterator = root.getNodes();

        List<String> expectedNodeNames = new ArrayList<>(2);
        expectedNodeNames.addAll(Arrays.asList("child1", "child2"));

        for (int i = 0; i < 2; i++) {
            assertTrue(iterator.hasNext());

            Node node = iterator.nextNode();
            assertTrue(expectedNodeNames.contains(node.getName()));

            expectedNodeNames.remove(node.getName());
        }

        assertFalse(iterator.hasNext());
    }

    @Test(expected = RepositoryException.class)
    public void importAtNodeWithChildren() throws IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-child.yaml");
        MockNodeFactory.importYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-child.yaml", root);
    }

    @Test
    public void importAdditionalNodes() throws IOException, RepositoryException {
        final MockNode root = MockNodeFactory.fromYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-child.yaml");
        final MockNode child = root.getNode("nodeWithChild/child");
        MockNodeFactory.importYaml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-children.yaml", child);

        assertEquals(1, child.getNodes().getSize());
        assertEquals(2, child.getNode("nodeWithChildren").getNodes().getSize());
        assertTrue(child.hasNode("nodeWithChildren/child1"));
        assertTrue(child.hasNode("nodeWithChildren/child2"));
    }

    private static void assertPathBelow(final Node base, final String expectedRelPath, String actualAbsPath) throws RepositoryException {
        final String expectedAbsPath = JcrPaths.getPath(base.getPath(), expectedRelPath).toString();
        assertEquals(expectedAbsPath, actualAbsPath);
    }
}
