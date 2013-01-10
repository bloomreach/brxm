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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        expected[0] = new MockValue("value1");
        expected[1] = new MockValue("value2");
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
        expected[0] = new MockValue("value2.1");
        expected[1] = new MockValue("value2.2");
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
        expected[0] = new MockValue("value");
        assertArrayEquals(expected, actual.getValues());
    }

    @Test
    public void propertyIsReturnedByIterator() throws RepositoryException {
        MockNode node = MockNode.root();
        node.setProperty("prop", "value");

        PropertyIterator iterator = node.getProperties();

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
        assertNull(prop.getParent());
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

}
