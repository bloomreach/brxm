/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;

import org.hippoecm.repository.api.RepositoryMap;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNodeFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RepositoryMapTest {

    private Node test;

    @Before
    public void setUp() throws Exception {
        test = MockNodeFactory.fromXml(getClass().getResource("RepositoryMapTest.xml"));
    }

    @Test
    public void testMapExists() {
        assertTrue(new RepositoryMapImpl(test).exists());
    }

    @Test
    public void testGetSubMap() {
        final Object foo = new RepositoryMapImpl(test).get("subnode");
        assertNotNull(foo);
        assertTrue(foo instanceof RepositoryMap);
    }

    @Test
    public void testGetSingleValuedProperty() {
        final Object value = new RepositoryMapImpl(test).get("single_property");
        assertNotNull(value);
        assertTrue(value instanceof String);
        assertEquals("value", value);
    }

    @Test
    public void testGetMultiValuedProperty() {
        final Object value = new RepositoryMapImpl(test).get("multi_property");
        assertNotNull(value);
        assertTrue(value instanceof Object[]);
        final Object[] values = (Object[]) value;
        assertEquals(3, values.length);
        assertEquals("value3", values[2]);
    }

    @Test
    public void testValues() {
        final Collection<Object> values = new RepositoryMapImpl(test).values();
        assertEquals(4, values.size());
    }

    @Test
    public void testEntrySet() {
        final Set<Map.Entry> entries = new RepositoryMapImpl(test).entrySet();
        assertEquals(4, entries.size());
    }

    @Test
    public void testSameNameItemPreferProperty() {
        assertEquals("value", new RepositoryMapImpl(test).get("same_name_item"));
    }

}
