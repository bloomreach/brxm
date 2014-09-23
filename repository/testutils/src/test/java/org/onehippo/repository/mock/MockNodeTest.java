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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.util.JcrConstants;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MockNodeTest {

    @Test
    public void emptyNode() throws RepositoryException {
        MockNode empty = MockNode.root();
        assertTrue(empty.isNode());
        assertEquals("/", empty.getPath());
        assertEquals("", empty.getName());
        assertEquals("rep:root", empty.getPrimaryNodeType().getName());
        assertFalse(empty.hasProperties());
        assertFalse(empty.getProperties().hasNext());
        assertEquals(0, empty.getDepth());
    }

    @Test
    public void copyConstructor() throws RepositoryException {
        MockNode node = MockNode.root().addNode("test", "nt:unstructured");
        node.setProperty("testProp", "foo");
        node.addMixin("testMixin");

        MockNode copy = new MockNode("copy", "newPrimaryType", node);
        copy.setParent(node.getParent());
        assertEquals("copy", copy.getName());
        assertEquals("newPrimaryType", copy.getPrimaryNodeType().getName());
        assertEquals("foo", copy.getProperty("testProp").getString());
        assertTrue(copy.isNodeType("testMixin"));

        // updating the original node should not affect the copy
        node.setProperty("testProp", "newValue");
        assertEquals("foo", copy.getProperty("testProp").getString());

        // removing a property from the copy should not affect the original node
        copy.removeProperty("testProp");
        assertFalse(copy.hasProperty("testProp"));
        assertTrue(node.hasProperty("testProp"));
    }

    @Test
    public void propertyIsSet() throws RepositoryException {
        MockNode node = MockNode.root();
        node.setProperty("prop", "value");

        assertTrue(node.hasProperty("prop"));
        Property actual = node.getProperty("prop");
        assertFalse(actual.isMultiple());
        assertEquals("value", actual.getString());
        assertEquals("/prop", actual.getPath());
        assertEquals(node, actual.getParent());
        assertEquals(1, actual.getDepth());
    }

    @Test
    public void propertyIsOverwritten() throws RepositoryException {
        MockNode node = MockNode.root();
        node.setProperty("prop", "value1");
        node.setProperty("prop", "value2");

        assertTrue(node.hasProperty("prop"));
        Property actual = node.getProperty("prop");
        assertFalse(actual.isMultiple());
        assertEquals("value2", actual.getString());
        assertEquals("/prop", actual.getPath());
        assertEquals(node, actual.getParent());
    }

    @Test
    public void multiplePropertyIsSet() throws RepositoryException {
        MockNode node = MockNode.root();
        String[] values = {"value1", "value2"};
        node.setProperty("prop", values);

        assertTrue(node.hasProperty("prop"));
        Property actual = node.getProperty("prop");
        assertTrue(actual.isMultiple());
        assertEquals("/prop", actual.getPath());
        assertEquals(node, actual.getParent());

        MockValue[] expected = new MockValue[2];
        expected[0] = new MockValue(PropertyType.STRING, "value1");
        expected[1] = new MockValue(PropertyType.STRING, "value2");
        assertArrayEquals(expected, actual.getValues());
    }

    @Test
    public void multiplePropertyIsOverwritten() throws RepositoryException {
        MockNode node = MockNode.root();
        String[] values1 = {"value1.1", "value1.2"};
        node.setProperty("prop", values1);
        String[] values2 = {"value2.1", "value2.2"};
        node.setProperty("prop", values2);

        assertTrue(node.hasProperty("prop"));
        Property actual = node.getProperty("prop");
        assertTrue(actual.isMultiple());

        MockValue[] expected = new MockValue[2];
        expected[0] = new MockValue(PropertyType.STRING, "value2.1");
        expected[1] = new MockValue(PropertyType.STRING, "value2.2");
        assertArrayEquals(expected, actual.getValues());
    }

    @Test
    public void multiplePropertyOfOneValue() throws RepositoryException {
        MockNode node = MockNode.root();
        String[] values = {"value"};
        node.setProperty("prop", values);

        assertTrue(node.hasProperty("prop"));
        Property actual = node.getProperty("prop");
        assertTrue(actual.isMultiple());

        MockValue[] expected = new MockValue[1];
        expected[0] = new MockValue(PropertyType.STRING, "value");
        assertArrayEquals(expected, actual.getValues());
    }

    @Test
    public void propertyIsReturnedByIterator() throws RepositoryException {
        MockNode node = MockNode.root();
        node.setProperty("prop", "value");

        PropertyIterator iterator = node.getProperties();

        assertEquals(1, iterator.getSize());
        assertTrue(iterator.hasNext());

        Property property = iterator.nextProperty();
        assertEquals("prop", property.getName());
        assertEquals("value", property.getString());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void propertiesAreReturnedByIterator() throws RepositoryException {
        MockNode node = MockNode.root();
        node.setProperty("prop1", "value1");
        node.setProperty("prop2", "value2");

        PropertyIterator iterator = node.getProperties();

        assertEquals(2, iterator.getSize());

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("prop1", "value1");
        expected.put("prop2", "value2");

        // Test whether all pairs in the map are returned property names and values.
        // The iterator can return the properties in random order, hence the remove-from-map-once-seen construct.
        for (int i = 0; i < 2; i++) {
            assertTrue(iterator.hasNext());

            Property property = iterator.nextProperty();

            String actualName = property.getName();
            assertTrue(expected.containsKey(actualName));

            String expectedValue = expected.get(actualName);
            assertEquals(expectedValue, property.getString());

            expected.remove(actualName);
        }

        assertFalse("There should be only 2 properties", iterator.hasNext());
    }

    @Test(expected = PathNotFoundException.class)
    public void unknownProperty() throws RepositoryException {
        MockNode node = MockNode.root();
        node.getProperty("noSuchProperty");
    }

    @Test
    public void propertyCanBeRemoved() throws RepositoryException {
        MockNode node = MockNode.root();
        node.setProperty("prop", "value");

        assertTrue(node.hasProperty("prop"));

        Property prop = node.getProperty("prop");
        prop.remove();

        assertFalse(node.hasProperty("prop"));
        assertNoParent("Removed property should not have a parent", prop);
    }

    @Test(expected = PathNotFoundException.class)
    public void childNotFound() throws RepositoryException {
        MockNode node = MockNode.root();
        node.getNode("noSuchChild");
    }

    @Test
    public void sessionSaveIsIgnored() throws RepositoryException {
        MockNode root = MockNode.root();
        Session session = root.getSession();
        assertNotNull(session);
        session.save();
    }

    @Test
    public void nodeCanBeAdded() throws RepositoryException {
        MockNode root = MockNode.root();

        MockNode child = new MockNode("child");
        root.addNode(child);

        assertEquals(1, root.getNodes().getSize());
        assertSame(child, root.getNode("child"));
        assertEquals(1, child.getDepth());
    }

    @Test
    public void nodeCanBeAddedWithPrimaryType() throws RepositoryException {
        MockNode root = MockNode.root();

        Node child = root.addNode("child", "nt:unstructured");
        assertEquals("nt:unstructured", child.getPrimaryNodeType().getName());

        assertEquals(1, root.getNodes().getSize());
        assertSame(child, root.getNode("child"));
    }

    @Test
    public void nodeCanBeAddedWithPrimaryTypeInConstructor() throws RepositoryException {
        MockNode node = new MockNode("child", "nt:unstructured");
        assertEquals("nt:unstructured", node.getPrimaryNodeType().getName());
    }

    @Test
    public void nodeCanAddedBeWithRelativePath() throws RepositoryException {
        MockNode root = MockNode.root();
        MockNode child = new MockNode("child");
        root.addNode(child);

        Node grandchild = root.addNode("child/grandchild", "nt:unstructured");
        assertEquals("nt:unstructured", grandchild.getPrimaryNodeType().getName());

        assertEquals("Root node should still have only one child", 1, root.getNodes().getSize());
        assertEquals("Child node should have one child", 1, child.getNodes().getSize());
        assertSame(grandchild, child.getNode("grandchild"));
        assertEquals(2, grandchild.getDepth());
    }

    @Test(expected = PathNotFoundException.class)
    public void nodeAddedWithRelativePathButMissingIntermediateNodeThrowsException() throws RepositoryException {
        MockNode root = MockNode.root();
        Node grandchild = root.addNode("child/grandchild", "nt:unstructured");
    }

    @Test
    public void hasIdentifier() {
        MockNode node = new MockNode("foo");
        assertNotNull("A mock node should have an identifier", node.getIdentifier());
    }

    @Test
    public void rootHasSpecificIdentifier() throws RepositoryException {
        assertEquals("cafebabe-cafe-babe-cafe-babecafebabe", MockNode.root().getIdentifier());
    }

    @Test
    public void identifierOfNodesInSameTreeIsUnique() throws RepositoryException {
        MockNode root = MockNode.root();

        MockNode node1 = new MockNode("one");
        root.addNode(node1);

        MockNode node2 = new MockNode("two");
        root.addNode(node2);

        assertFalse("A mock node should have a identifier that is unique in the tree it is part of", node1.getIdentifier().equals(node2.getIdentifier()));
    }

    @Test
    public void rootIsOfTypeRepRoot() throws RepositoryException {
        assertTrue("Root node should be of type 'rep:root'", MockNode.root().isNodeType("rep:root"));
    }

    @Test
    public void nodeTypeMatchesPrimaryType() throws RepositoryException {
        MockNode node = new MockNode("test");
        node.setPrimaryType("my:type");
        assertTrue("Node should be of type equal to its primary type", node.isNodeType("my:type"));
    }

    @Test
    public void nodeWithoutPrimaryTypeIsNoType() {
        MockNode node = new MockNode("test");
        assertFalse(node.isNodeType("some:type"));
    }

    @Test
    public void childNodeCanBeRemoved() throws RepositoryException {
        final MockNode root = MockNode.root();

        Node child = root.addNode("child", "nt:unstructured");
        assertEquals("Child node should have been added", 1, root.getNodes().getSize());

        child.remove();
        assertFalse("Root should not have the removed node as a child", root.hasNode("child"));
        assertEquals("Removed child should no longer exist", 0, root.getNodes().getSize());
        assertNoParent("Removed child should not have a parent", child);
    }

    @Test
    public void rootCanBeRemoved() throws RepositoryException {
        MockNode.root().remove();
    }

    @Test
    public void iteratedChildrenCanBeRemoved() throws RepositoryException {
        final MockNode root = MockNode.root();
        root.addNode("child1", "nt:unstructured");
        root.addNode("child2", "nt:unstructured");

        NodeIterator children = root.getNodes();
        while (children.hasNext()) {
            children.nextNode().remove();
        }

        assertEquals(0, root.getNodes().getSize());
    }

    @Test
    public void singlePropertyHasDefinition() throws RepositoryException {
        final MockNode root = MockNode.root();
        root.setProperty("test", "value");

        final PropertyDefinition definition = root.getProperty("test").getDefinition();

        assertEquals("test", definition.getName());
        assertFalse("Single property should not be defined as multiple", definition.isMultiple());
    }

    @Test
    public void multiplePropertyHasDefinition() throws RepositoryException {
        final MockNode root = MockNode.root();
        final String[] values = {"one", "two"};
        root.setProperty("test", values);

        final PropertyDefinition definition = root.getProperty("test").getDefinition();

        assertEquals("test", definition.getName());
        assertTrue("Multiple property should not defined as multiple", definition.isMultiple());
    }

    @Test(expected = ItemNotFoundException.class)
    public void primaryItemNotDefined() throws RepositoryException {
        MockNode.root().getPrimaryItem();
    }

    @Test(expected = ItemNotFoundException.class)
    public void primaryItemNodeDoesNotExist() throws RepositoryException {
        final MockNode root = MockNode.root();
        root.setPrimaryItemName("child");
        root.getPrimaryItem();
    }

    @Test
    public void primaryItemNode() throws RepositoryException {
        final MockNode root = MockNode.root();
        root.setPrimaryItemName("child");
        final Node child = root.addNode("child", "nt:unstructured");

        final Item primaryItem = root.getPrimaryItem();
        assertSame(child, primaryItem);
    }

    @Test
    public void primaryItemProperty() throws RepositoryException {
        final MockNode root = MockNode.root();
        root.setPrimaryItemName("prop");
        root.setProperty("prop", "value");

        final Item primaryItem = root.getPrimaryItem();
        assertFalse("Primary item should be a property", primaryItem.isNode());
        assertEquals("prop", primaryItem.getName());
    }

    @Test
    public void primaryNodeTypeKnowsPrimaryItemNameIsNotSet() throws RepositoryException {
        final MockNode root = MockNode.root();
        assertNull(root.getPrimaryNodeType().getPrimaryItemName());
    }

    @Test
    public void primaryNodeTypeKnowsPrimaryItemName() throws RepositoryException {
        final MockNode root = MockNode.root();
        root.setPrimaryItemName("foo");
        assertEquals("foo", root.getPrimaryNodeType().getPrimaryItemName());
    }

    @Test
    public void snsNodesSupportedByDefault() throws RepositoryException {
        MockNode root = MockNode.root();
        MockNode folder1 = root.addNode("folder1", "nt:unstructured");
        MockNode folder2 = folder1.addNode("folder2", "nt:unstructured");
        folder2 = folder1.addNode("folder2", "nt:unstructured");
        MockNode sns1 = folder2.addNode("sns", "un:unstructured");
        assertEquals("sns", sns1.getName());
        assertEquals("/folder1/folder2[2]/sns", sns1.getPath());
        MockNode sns2 = folder2.addNode("sns", "un:unstructured");
        assertEquals("sns", sns2.getName());
        assertEquals("/folder1/folder2[2]/sns[2]", sns2.getPath());
        assertNotSame(sns1, sns2);
        assertEquals(sns1.getName(), sns2.getName());
    }

    @Test
    public void snsNodesSupportingDisabled() throws RepositoryException {
        MockNode root = MockNode.root();
        MockNode folder1 = root.addNode("folder1", "nt:unstructured");
        folder1.setSameNameSiblingSupported(false);
        MockNode folder2 = folder1.addNode("folder2", "nt:unstructured");

        try {
            folder2 = folder1.addNode("folder2", "nt:unstructured");
            fail("SNS node disabled.");
        } catch (ConstraintViolationException e) {
            // as expected.
        }
    }

    @Test
    public void getNodesWithNamePattern() throws RepositoryException {
        MockNode root = MockNode.root();
        MockNode folder = root.addNode("folder1", "nt:unstructured");
        MockNode handle = folder.addNode("document1", "nt:unstructured");
        handle.addNode("document1", "nt:unstructured");
        handle.addNode("document1", "nt:unstructured");
        handle.addNode("document1", "nt:unstructured");
        handle.addNode("hippo:request", "nt:unstructured");

        Set<String> variantPaths = new HashSet<String>();
        NodeIterator nodeIt = handle.getNodes("document*");
        assertEquals(3, nodeIt.getSize());

        while (nodeIt.hasNext()) {
            variantPaths.add(nodeIt.nextNode().getPath());
        }

        assertTrue(variantPaths.contains("/folder1/document1/document1"));
        assertTrue(variantPaths.contains("/folder1/document1/document1[2]"));
        assertTrue(variantPaths.contains("/folder1/document1/document1[3]"));

        variantPaths.clear();

        nodeIt = handle.getNodes("document*|hippo:*");
        assertEquals(4, nodeIt.getSize());

        while (nodeIt.hasNext()) {
            variantPaths.add(nodeIt.nextNode().getPath());
        }

        assertTrue(variantPaths.contains("/folder1/document1/document1"));
        assertTrue(variantPaths.contains("/folder1/document1/document1[2]"));
        assertTrue(variantPaths.contains("/folder1/document1/document1[3]"));
        assertTrue(variantPaths.contains("/folder1/document1/hippo:request"));

        variantPaths.clear();

        nodeIt = handle.getNodes(new String [] { "document*", "hippo:*", "doc*" });
        assertEquals(4, nodeIt.getSize());

        while (nodeIt.hasNext()) {
            variantPaths.add(nodeIt.nextNode().getPath());
        }

        assertTrue(variantPaths.contains("/folder1/document1/document1"));
        assertTrue(variantPaths.contains("/folder1/document1/document1[2]"));
        assertTrue(variantPaths.contains("/folder1/document1/document1[3]"));
        assertTrue(variantPaths.contains("/folder1/document1/hippo:request"));
    }

    @Test
    public void getPropertiesWithNamePattern() throws RepositoryException {
        MockNode root = MockNode.root();
        MockNode node = root.addNode("node1", "nt:unstructured");
        node.setProperty("prop1", "value1");
        node.setProperty("prop2", "value2");
        node.setProperty("hippo:holder", "editor");

        Set<String> propPaths = new HashSet<String>();
        PropertyIterator propIt = node.getProperties("prop*");
        assertEquals(2, propIt.getSize());

        while (propIt.hasNext()) {
            propPaths.add(propIt.nextProperty().getPath());
        }

        assertTrue(propPaths.contains("/node1/prop1"));
        assertTrue(propPaths.contains("/node1/prop2"));

        propPaths.clear();

        propIt = node.getProperties("prop* | hippo:*");
        assertEquals(3, propIt.getSize());

        while (propIt.hasNext()) {
            propPaths.add(propIt.nextProperty().getPath());
        }

        assertTrue(propPaths.contains("/node1/prop1"));
        assertTrue(propPaths.contains("/node1/prop2"));
        assertTrue(propPaths.contains("/node1/hippo:holder"));

        propPaths.clear();

        propIt = node.getProperties(new String [] { "prop*", "hippo:*", "pro*" });
        assertEquals(3, propIt.getSize());

        while (propIt.hasNext()) {
            propPaths.add(propIt.nextProperty().getPath());
        }

        assertTrue(propPaths.contains("/node1/prop1"));
        assertTrue(propPaths.contains("/node1/prop2"));
        assertTrue(propPaths.contains("/node1/hippo:holder"));
    }

    @Test
    public void testVariousTypesOfProperties() throws RepositoryException, IOException {
        MockNode root = MockNode.root();
        MockNode node = root.addNode("node1", "nt:unstructured");

        Calendar now = Calendar.getInstance();
        byte[] binaryData = new byte[1];

        node.setProperty("string1", "stringvalue1");
        node.setProperty("boolean1", true);
        node.setProperty("long1", Long.MAX_VALUE);
        node.setProperty("date1", now);
        node.setProperty("double1", Double.MAX_VALUE);
        node.setProperty("bigdecimal1", BigDecimal.TEN);
        node.setProperty("binary", new MockBinary(new ByteArrayInputStream(binaryData)));

        assertTrue(node.getProperty("boolean1").getBoolean());
        assertEquals(Long.MAX_VALUE, node.getProperty("long1").getLong());
        assertEquals(ISO8601.format(now), ISO8601.format(node.getProperty("date1").getDate()));
        assertEquals(Double.toString(Double.MAX_VALUE), Double.toString(node.getProperty("double1").getDouble()));
        assertEquals(BigDecimal.TEN, node.getProperty("bigdecimal1").getDecimal());
        assertEquals("stringvalue1", node.getProperty("string1").getString());
        assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(binaryData), node.getProperty("binary").getBinary().getStream()));

        node.setProperty("stringarray1", new String [] { "stringvalue1", "stringvalue2" });
        node.setProperty("booleanarray1", new MockValue[] { new MockValue(PropertyType.BOOLEAN, "true"), new MockValue(PropertyType.BOOLEAN, "false") });
        node.setProperty("longarray1", new MockValue[] { new MockValue(PropertyType.LONG, "123"), new MockValue(PropertyType.LONG, "456") });
        node.setProperty("datearray1", new MockValue[] { new MockValue(PropertyType.DATE, "2013-10-30T00:00:00.000Z"), new MockValue(PropertyType.DATE, "2013-10-31T00:00:00.000Z") });
        node.setProperty("doublearray1", new MockValue[] { new MockValue(PropertyType.DOUBLE, "1.23"), new MockValue(PropertyType.DOUBLE, "4.56") });
        node.setProperty("bigdecimalarray1", new MockValue[] { new MockValue(PropertyType.DECIMAL, "1.23E3"), new MockValue(PropertyType.DECIMAL, "4.56E3") });

        Value [] values = node.getProperty("stringarray1").getValues();
        assertEquals(2, values.length);
        assertEquals("stringvalue1", values[0].getString());
        assertEquals("stringvalue2", values[1].getString());

        values = node.getProperty("booleanarray1").getValues();
        assertEquals(2, values.length);
        assertEquals(true, values[0].getBoolean());
        assertEquals(false, values[1].getBoolean());

        values = node.getProperty("longarray1").getValues();
        assertEquals(2, values.length);
        assertEquals(123, values[0].getLong());
        assertEquals(456, values[1].getLong());

        values = node.getProperty("datearray1").getValues();
        assertEquals(2, values.length);
        assertEquals("2013-10-30T00:00:00.000Z", ISO8601.format(values[0].getDate()));
        assertEquals("2013-10-31T00:00:00.000Z", ISO8601.format(values[1].getDate()));

        values = node.getProperty("doublearray1").getValues();
        assertEquals(2, values.length);
        assertEquals(Double.toString(1.23d), Double.toString(values[0].getDouble()));
        assertEquals(Double.toString(4.56), Double.toString(values[1].getDouble()));

        values = node.getProperty("bigdecimalarray1").getValues();
        assertEquals(2, values.length);
        assertEquals(new BigDecimal("1.23E3"), values[0].getDecimal());
        assertEquals(new BigDecimal("4.56E3"), values[1].getDecimal());
    }

    @Test
    public void testGetNodeWithRelPath() throws Exception {
        MockNode root = MockNode.root();
        MockNode folder = root.addNode("folder1", "nt:unstructured");
        folder.addNode("node1", "nt:unstructured");
        MockNode node2 = folder.addNode("node2", "nt:unstructured");
        MockNode node21 = node2.addNode("node21", "nt:unstructured");
        node21.setProperty("tag", "node21");
        MockNode node22 = node2.addNode("node22", "nt:unstructured");
        node22.setProperty("tag", "node22");
        MockNode node22sns = node2.addNode("node22", "nt:unstructured");
        node22sns.setProperty("tag", "node22sns");

        assertNotNull(root.getNode("folder1"));
        assertNotNull(folder.getNode("node1"));
        assertNotNull(folder.getNode("node2"));
        assertNotNull(folder.getNode("node2/node21"));
        assertEquals("node21", folder.getNode("node2/node21").getProperty("tag").getString());
        assertNotNull(folder.getNode("node2/node22"));
        assertEquals("node22", folder.getNode("node2/node22").getProperty("tag").getString());
        assertNotNull(folder.getNode("node2/node22[1]"));
        assertEquals("node22", folder.getNode("node2/node22[1]").getProperty("tag").getString());
        assertNotNull(folder.getNode("node2/node22[2]"));
        assertEquals("node22sns", folder.getNode("node2/node22[2]").getProperty("tag").getString());
    }

    @Test
    public void testHasNodeWithRelPath() throws Exception {
        MockNode root = MockNode.root();
        MockNode folder = root.addNode("folder1", "nt:unstructured");
        folder.addNode("node1", "nt:unstructured");
        MockNode node2 = folder.addNode("node2", "nt:unstructured");
        MockNode node21 = node2.addNode("node21", "nt:unstructured");
        node21.setProperty("tag", "node21");
        MockNode node22 = node2.addNode("node22", "nt:unstructured");
        node22.setProperty("tag", "node22");
        MockNode node22sns = node2.addNode("node22", "nt:unstructured");
        node22sns.setProperty("tag", "node22sns");

        assertTrue(root.hasNode("folder1"));
        assertTrue(folder.hasNode("node1"));
        assertTrue(folder.hasNode("node2"));
        assertTrue(folder.hasNode("node2/node21"));
        assertTrue(folder.hasNode("node2/node22"));
        assertTrue(folder.hasNode("node2/node22[1]"));
        assertTrue(folder.hasNode("node2/node22[2]"));
    }

    @Test
    public void testGetPropertyWithRelPath() throws Exception {
        MockNode root = MockNode.root();
        MockNode folder = root.addNode("folder1", "nt:unstructured");
        folder.addNode("node1", "nt:unstructured");
        MockNode node2 = folder.addNode("node2", "nt:unstructured");
        MockNode node21 = node2.addNode("node21", "nt:unstructured");
        node21.setProperty("tag", "node21");
        MockNode node22 = node2.addNode("node22", "nt:unstructured");
        node22.setProperty("tag", "node22");
        MockNode node22sns = node2.addNode("node22", "nt:unstructured");
        node22sns.setProperty("tag", "node22sns");

        assertEquals("node21", folder.getProperty("node2/node21/tag").getString());
        assertEquals("node22", folder.getProperty("node2/node22/tag").getString());
        assertEquals("node22", folder.getProperty("node2/node22[1]/tag").getString());
        assertEquals("node22sns", folder.getProperty("node2/node22[2]/tag").getString());
    }

    @Test
    public void testHasPropertyWithRelPath() throws Exception {
        MockNode root = MockNode.root();
        MockNode folder = root.addNode("folder1", "nt:unstructured");
        folder.addNode("node1", "nt:unstructured");
        MockNode node2 = folder.addNode("node2", "nt:unstructured");
        MockNode node21 = node2.addNode("node21", "nt:unstructured");
        node21.setProperty("tag", "node21");
        MockNode node22 = node2.addNode("node22", "nt:unstructured");
        node22.setProperty("tag", "node22");
        MockNode node22sns = node2.addNode("node22", "nt:unstructured");
        node22sns.setProperty("tag", "node22sns");

        assertTrue(folder.hasProperty("node2/node21/tag"));
        assertTrue(folder.hasProperty("node2/node22/tag"));
        assertTrue(folder.hasProperty("node2/node22[1]/tag"));
        assertTrue(folder.hasProperty("node2/node22[2]/tag"));
    }

    @Test(expected = UnsupportedRepositoryOperationException.class)
    public void checkinNonVersionableNode() throws RepositoryException {

        final MockNode root = MockNode.root();
        final VersionManager versionManager = root.getSession().getWorkspace().getVersionManager();
        versionManager.checkin(root.getPath());
    }

    @Test
    public void checkinVersionableNode() throws RepositoryException {
        MockNode node = MockNode.root().addNode("test", "nt:base");
        node.addMixin("mix:versionable");
        assertTrue(node.isCheckedOut());
        assertTrue(node.getProperty(JcrConstants.JCR_IS_CHECKED_OUT).getBoolean());
        final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        versionManager.checkin(node.getPath());
        assertFalse(node.isCheckedOut());
        assertFalse(node.getProperty(JcrConstants.JCR_IS_CHECKED_OUT).getBoolean());
    }


    @Test
    public void frozenNode_test_frozen_uuid_and_node_types() throws RepositoryException, IOException {
        MockNode node = MockNode.root().addNode("node", "nt:unstructured");
        node.addMixin("mix:versionable");
        final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        final Version versioned = versionManager.checkin(node.getPath());
        final Node frozenNode = versioned.getFrozenNode();
        assertEquals(node.getIdentifier(), frozenNode.getProperty(JcrConstants.JCR_FROZEN_UUID).getString());
        assertEquals("nt:unstructured", frozenNode.getProperty(JcrConstants.JCR_FROZEN_PRIMARY_TYPE).getString());
        assertFalse("mix:versionable should be skipped from frozen node",frozenNode.hasProperty(JcrConstants.JCR_FROZEN_MIXIN_TYPES));

    }

    @Test
    public void frozenNode_test_frozen_uuid_primary_with_mixin_types_besides_versionable() throws RepositoryException, IOException {
        MockNode node = MockNode.root().addNode("node", "nt:unstructured");
        node.addMixin("mix:versionable");
        node.addMixin("mix:etag");

        final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        final Version versioned = versionManager.checkin(node.getPath());
        final Node frozenNode = versioned.getFrozenNode();

        assertEquals(node.getIdentifier(), frozenNode.getProperty(JcrConstants.JCR_FROZEN_UUID).getString());
        assertEquals("nt:unstructured", frozenNode.getProperty(JcrConstants.JCR_FROZEN_PRIMARY_TYPE).getString());
        assertTrue(frozenNode.hasProperty(JcrConstants.JCR_FROZEN_MIXIN_TYPES));
        assertEquals("mix:etag", frozenNode.getProperty(JcrConstants.JCR_FROZEN_MIXIN_TYPES).getValues()[0].getString());
    }

    @Test
    public void frozenNode_test_various_property_types() throws RepositoryException, IOException {
        MockNode node = MockNode.root().addNode("test", "nt:unstructured");
        node.addMixin("mix:versionable");
        node.setProperty("string", "string");
        node.setProperty("true", true);
        node.setProperty("false", false);
        node.setProperty("long", 10L);
        node.setProperty("binary", new MockBinary(IOUtils.toInputStream("hello world!")));
        node.setProperty("double", 10D);
        final BigDecimal bigDecimal = new BigDecimal(1.0000000012123d);
        node.setProperty("bigDecimal", bigDecimal);
        final Calendar cal = Calendar.getInstance();
        node.setProperty("cal", cal);

        final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        final Version version = versionManager.checkin(node.getPath());

        assertEquals("string", version.getFrozenNode().getProperty("string").getString());
        assertEquals(true, version.getFrozenNode().getProperty("true").getBoolean());
        assertEquals(false, version.getFrozenNode().getProperty("false").getBoolean());
        assertEquals(10L, version.getFrozenNode().getProperty("long").getLong());
        assertEquals("hello world!", IOUtils.toString(version.getFrozenNode().getProperty("binary").getBinary().getStream()));
        assertEquals(10D, version.getFrozenNode().getProperty("double").getDouble(), 001D);
        assertEquals(bigDecimal, version.getFrozenNode().getProperty("bigDecimal").getDecimal());
        assertEquals(ISO8601.format(cal), ISO8601.format(node.getProperty("cal").getDate()));
    }

    @Test
    public void cannotModifyCheckedInNode() throws RepositoryException {
        final MockNode node = MockNode.root().addNode("test", "nt:base");
        node.addMixin("mix:versionable");
        node.setProperty("test", "foo");
        node.addNode("child", "nt:unstructured");
        final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        versionManager.checkin(node.getPath());
        assertVersionException(new Code() {
            public void execute() throws RepositoryException {
                node.setProperty("test", "bar");
            }
        });
        assertVersionException(new Code() {
            public void execute() throws RepositoryException {
                node.getProperty("test").remove();
            }
        });
        assertVersionException(new Code() {
            public void execute() throws RepositoryException {
                node.addNode("foo", "nt:unstructured");
            }
        });
        assertVersionException(new Code() {
            public void execute() throws RepositoryException {
                node.getNode("child").remove();
            }
        });
        assertVersionException(new Code() {
            public void execute() throws RepositoryException {
                node.remove();
            }
        });
        assertVersionException(new Code() {
            public void execute() throws RepositoryException {
                node.setPrimaryType("bar");
            }
        });
    }

    @Test(expected = UnsupportedRepositoryOperationException.class)
    public void checkoutNonVersionableNode() throws RepositoryException {

        final MockNode root = MockNode.root();
        final VersionManager versionManager = root.getSession().getWorkspace().getVersionManager();
        versionManager.checkout(root.getPath());
    }

    @Test
    public void checkoutCheckedInNode() throws RepositoryException {
        MockNode node = MockNode.root().addNode("test", "nt:base");
        node.addMixin("mix:versionable");

        final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        versionManager.checkin(node.getPath());
        versionManager.checkout(node.getPath());

        assertTrue(node.isCheckedOut());
        assertTrue(node.getProperty(JcrConstants.JCR_IS_CHECKED_OUT).getBoolean());
    }

    @Test
    public void canModifyCheckedOutNode() throws RepositoryException {
        final MockNode root = MockNode.root();
        final MockNode node = root.addNode("test", "nt:base");
        node.addMixin("mix:versionable");
        final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        versionManager.checkin(node.getPath());
        versionManager.checkout(node.getPath());

        node.setProperty("testProp", "bar");
        assertTrue(node.hasProperty("testProp"));
        final Property test = node.getProperty("testProp");
        assertEquals("bar", test.getString());
        test.remove();
        assertFalse(node.hasProperty("test"));

        assertFalse(node.hasNode("foo"));
        node.addNode("foo", "nt:unstructured");
        assertTrue(node.hasNode("foo"));

        assertTrue(root.hasNode("test"));
        node.remove();
        assertFalse(root.hasNode("test"));
    }

    @Test
    public void getRootVersion() throws RepositoryException, InterruptedException {
        final MockNode node = MockNode.root().addNode("test", "nt:base");
        node.addMixin("mix:versionable");

        final MockVersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        final MockVersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());
        final Version rootVersion = versionHistory.getRootVersion();
        final Node frozenNode = rootVersion.getFrozenNode();

        assertEquals(node.getIdentifier(), frozenNode.getProperty(JcrConstants.JCR_FROZEN_UUID).getString());
    }

    @Test
    public void getAllVersions() throws RepositoryException, InterruptedException {
        final MockNode node = MockNode.root().addNode("test", "nt:base");
        node.addMixin("mix:versionable");
        node.setProperty("testProp", "a");

        final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        versionManager.checkin(node.getPath());
        versionManager.checkout(node.getPath());
        node.setProperty("testProp", "b");
        versionManager.checkin(node.getPath());

        final VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());
        final VersionIterator allVersions = versionHistory.getAllVersions();

        assertEquals(2, allVersions.getSize());

        final Version a = allVersions.nextVersion();
        assertEquals("b", a.getFrozenNode().getProperty("testProp").getString());

        final Version b = allVersions.nextVersion();
        assertEquals("a", b.getFrozenNode().getProperty("testProp").getString());
    }

    @Test
    public void getVersion() throws RepositoryException {
        final MockNode node = MockNode.root().addNode("test", "nt:base");
        node.addMixin("mix:versionable");
        node.setProperty("testProp", "a");
        final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
        versionManager.checkin(node.getPath());

        final VersionHistory versionHistory = versionManager.getVersionHistory(node.getPath());

        final Version version = versionHistory.getVersion("1.0");

        assertEquals("a", version.getFrozenNode().getProperty("testProp").getString());
    }

    private static void assertNoParent(String message, Item item) throws RepositoryException {
        try {
            item.getParent();
            fail(message);
        } catch (ItemNotFoundException expected) {
            // everything is fine
        }
    }

    private static void assertVersionException(Code code) throws RepositoryException {
        try {
            code.execute();
            fail("Expected a VersionException to be thrown");
        } catch (VersionException expected) {
        }
    }

    private interface Code {
        public void execute() throws RepositoryException;
    }

}
