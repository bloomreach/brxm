/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

public class MockNodeFactoryTest {

    @Test(expected = IOException.class)
    public void resourceDoesNotExist() throws RepositoryException, JAXBException, IOException {
        MockNodeFactory.fromXml("/no/such/resource.xml");
    }

    @Test
    public void emptyNode() throws JAXBException, IOException, RepositoryException {
        MockNode root = MockNodeFactory.fromXml(getClass().getResource("MockNodeFactoryTest-empty-node.xml"));

        assertEquals("", root.getName());
        assertEquals("/", root.getPath());
        assertEquals("nt:unstructured", root.getPrimaryNodeType().getName());
    }

    @Test
    public void stringProperty() throws JAXBException, IOException, RepositoryException {
        MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.xml");

        Property property = root.getProperty("stringProperty");
        assertFalse(property.isMultiple());
        assertEquals("aaa", property.getString());
        assertEquals("/stringProperty", property.getPath());
    }

    @Test
    public void stringPropertyNotMultiple() throws JAXBException, IOException, RepositoryException {
        MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.xml");

        Property property = root.getProperty("stringPropertyNotMultiple");
        assertFalse(property.isMultiple());
        assertEquals("/stringPropertyNotMultiple", property.getPath());
        assertEquals("bbb", property.getString());
    }

    @Test
    public void multipleStringProperty() throws JAXBException, IOException, RepositoryException {
        MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.xml");

        Property property = root.getProperty("multipleStringProperty");
        assertTrue(property.isMultiple());
        assertEquals("/multipleStringProperty", property.getPath());

        Value[] expected = new MockValue[2];
        expected[0] = new MockValue(PropertyType.STRING, "ccc");
        expected[1] = new MockValue(PropertyType.STRING, "ddd");
        assertArrayEquals(expected, property.getValues());
    }

    @Test
    public void multipleStringPropertyWithOneValue() throws JAXBException, IOException, RepositoryException {
        MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-properties.xml");

        Property property = root.getProperty("multipleStringPropertyWithOneValue");
        assertTrue(property.isMultiple());
        assertEquals("/multipleStringPropertyWithOneValue", property.getPath());

        Value[] expected = new MockValue[1];
        expected[0] = new MockValue(PropertyType.STRING, "eee");
        assertArrayEquals(expected, property.getValues());
    }

    @Test
    public void nodeWithChild() throws JAXBException, IOException, RepositoryException {
        MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-child.xml");

        Property parentProperty = root.getProperty("parentProperty");
        assertFalse(parentProperty.isMultiple());
        assertEquals("/parentProperty", parentProperty.getPath());
        assertEquals("parentValue", parentProperty.getValue().getString());

        assertTrue(root.hasNode("child"));

        Node child = root.getNode("child");
        assertEquals("child", child.getName());
        assertEquals("/child", child.getPath());
        assertEquals(root, child.getParent());
        assertTrue(child.hasProperties());
        assertTrue(child.hasProperty("childProperty"));

        Property childProperty = child.getProperty("childProperty");
        assertFalse(childProperty.isMultiple());
        assertEquals("/child/childProperty", childProperty.getPath());
        assertEquals("childValue", childProperty.getString());
    }

    @Test
    public void nodeWithChildren() throws JAXBException, IOException, RepositoryException {
        MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-children.xml");

        assertTrue(root.hasNode("child1"));
        assertTrue(root.hasNode("child2"));

        Node firstChild = root.getNode("child1");
        assertEquals("child1", firstChild.getName());
        assertEquals("/child1", firstChild.getPath());
        assertEquals(root, firstChild.getParent());
        assertTrue(firstChild.hasProperties());
        assertTrue(firstChild.hasProperty("childProperty"));

        Property firstChildProperty = firstChild.getProperty("childProperty");
        assertFalse(firstChildProperty.isMultiple());
        assertEquals("/child1/childProperty", firstChildProperty.getPath());
        assertEquals("value1", firstChildProperty.getString());

        Node secondChild = root.getNode("child2");
        assertEquals("child2", secondChild.getName());
        assertEquals("/child2", secondChild.getPath());
        assertEquals(root, secondChild.getParent());
        assertTrue(secondChild.hasProperties());
        assertTrue(secondChild.hasProperty("childProperty"));

        Property secondChildProperty = secondChild.getProperty("childProperty");
        assertFalse(secondChildProperty.isMultiple());
        assertEquals("/child2/childProperty", secondChildProperty.getPath());
        assertEquals("value2", secondChildProperty.getString());
    }

    @Test
    public void nodeIteratorReturnsAllChildren() throws JAXBException, IOException, RepositoryException {
        MockNode root = MockNodeFactory.fromXml("/org/onehippo/repository/mock/MockNodeFactoryTest-node-with-children.xml");

        NodeIterator iterator = root.getNodes();

        List<String> expectedNodeNames = new ArrayList<String>(2);
        expectedNodeNames.addAll(Arrays.asList("child1", "child2"));

        for (int i = 0; i < 2; i++) {
            assertTrue(iterator.hasNext());

            Node node = iterator.nextNode();
            assertTrue(expectedNodeNames.contains(node.getName()));

            expectedNodeNames.remove(node.getName());
        }

        assertFalse(iterator.hasNext());
    }

}
