/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class PlainYamlObjectTest {

    @Test
    public void testEmptyYaml() {
        PlainYamlObject pyo = new PlainYamlObject("");
        assertFalse(pyo.asObject().isPresent());
        assertFalse(pyo.asObject(null).isPresent());
        assertFalse(pyo.asObject("").isPresent());
        assertFalse(pyo.asObject("foo").isPresent());
        assertFalse(pyo.asMap().isPresent());
        assertFalse(pyo.asMap(null).isPresent());
        assertFalse(pyo.asMap("").isPresent());
        assertFalse(pyo.asMap("foo").isPresent());
        assertFalse(pyo.asList().isPresent());
        assertFalse(pyo.asList(null).isPresent());
        assertFalse(pyo.asList("").isPresent());
        assertFalse(pyo.asList("foo").isPresent());
    }

    @Test
    public void testYamlMap() {
        PlainYamlObject pyo = new PlainYamlObject("foo:\n  one: 1\n  two: two");
        assertTrue(pyo.asObject().isPresent());
        assertTrue(pyo.asObject(null).isPresent());
        assertTrue(pyo.asObject("").isPresent());
        assertFalse(pyo.asList().isPresent());
        assertTrue(Map.class.isInstance(pyo.asObject().get()));
        assertTrue(pyo.asMap().get().containsKey("foo"));
        assertFalse(pyo.asMap().get().containsKey("bar"));
        assertTrue(pyo.asMap("foo").get().containsKey("one"));
        assertTrue(pyo.asMap("foo").get().containsKey("two"));
        assertTrue(Integer.class.isInstance(pyo.asObject("foo/one").get()));
        assertEquals(pyo.asObject("foo/one").get(), 1);
        assertTrue(String.class.isInstance(pyo.asObject("foo/two").get()));
        assertEquals(pyo.asObject("foo/two").get(), "two");
        assertFalse(pyo.asMap("foo").get().containsKey("three"));
        assertFalse(pyo.asMap("bar").isPresent());
        assertFalse(pyo.asObject("foo/one/bar").isPresent());
    }

    @Test
    public void testYamlList() {
        PlainYamlObject pyo = new PlainYamlObject("- foo\n- bar\n- 3\n");
        assertTrue(pyo.asObject().isPresent());
        assertTrue(pyo.asObject(null).isPresent());
        assertTrue(pyo.asObject("").isPresent());
        assertFalse(pyo.asMap().isPresent());
        assertTrue(List.class.isInstance(pyo.asObject().get()));
        assertTrue(pyo.asList().get().contains("foo"));
        assertTrue(pyo.asList().get().contains("bar"));
        assertTrue(pyo.asList().get().contains(3));
        assertFalse(pyo.asList().get().contains("one"));
        assertFalse(pyo.asList().get().contains(4));
    }

    @Test
    public void testYamlMappedList() {
        // sequence mapped to list
        PlainYamlObject pyo = new PlainYamlObject("map:\n  list:\n    - foo\n    - bar\n");
        assertTrue(pyo.asList("map/list").isPresent());
        assertTrue(pyo.asList("map/list").get().contains("foo"));
        assertTrue(pyo.asList("map/list").get().contains("bar"));
        assertFalse(pyo.asList("map/list").get().contains("one"));
        // array mapped to list
        pyo = new PlainYamlObject("map:\n  list: [foo, bar]\n");
        assertTrue(pyo.asList("map/list").isPresent());
        assertTrue(pyo.asList("map/list").get().contains("foo"));
        assertTrue(pyo.asList("map/list").get().contains("bar"));
        assertFalse(pyo.asList("map/list").get().contains("one"));
    }
}
