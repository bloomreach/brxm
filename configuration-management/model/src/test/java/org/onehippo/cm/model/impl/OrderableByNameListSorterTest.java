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
package org.onehippo.cm.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onehippo.cm.model.OrderableByName;
import org.onehippo.cm.model.impl.exceptions.CircularDependencyException;
import org.onehippo.cm.model.impl.exceptions.DuplicateNameException;
import org.onehippo.cm.model.impl.exceptions.MissingDependencyException;

import static org.junit.Assert.assertEquals;

public class OrderableByNameListSorterTest {

    private static final OrderableByNameListSorter<OrderableByName> orderableSorter = new OrderableByNameListSorter<>(OrderableByName.class);

    /*
     * test duplicate name detection
     */

    @Test
    public void duplicate_names() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("o1", "");
        OrderableByNameImpl o2 = new OrderableByNameImpl("o1", "");

        try {
            orderableSorter.sort(list(o1, o2));
        } catch (DuplicateNameException e) {
            assertEquals("Duplicate OrderableByName named 'o1'.", e.getMessage());
        }
    }

    /*
     * test circular dependency detection
     */

    @Test
    public void self_circular_dependency() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("o1", "o1");
        OrderableByNameImpl o2 = new OrderableByNameImpl("o2", "");

        try {
            orderableSorter.sort(list(o1, o2));
        } catch (CircularDependencyException e) {
            assertEquals("OrderableByName 'o1' has a circular dependency: [o1 -> o1].", e.getMessage());
        }
    }

    @Test
    public void two_wise_circular_dependency() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("o1","o2");
        OrderableByNameImpl o2 = new OrderableByNameImpl("o2","o1");

        try {
            orderableSorter.sort(list(o1, o2));
        } catch (CircularDependencyException e) {
            assertEquals("OrderableByName 'o1' has a circular dependency: [o1 -> o2 -> o1].", e.getMessage());
        }
    }

    @Test
    public void three_wise_circular_dependency() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("o1","o2");
        OrderableByNameImpl o2 = new OrderableByNameImpl("o2","o3");
        OrderableByNameImpl o3 = new OrderableByNameImpl("o3","o1");

        try {
            orderableSorter.sort(list(o1, o2, o3));
        } catch (CircularDependencyException e) {
            assertEquals("OrderableByName 'o1' has a circular dependency: [o1 -> o2 -> o3 -> o1].", e.getMessage());
        }
    }

    @Test
    public void complex_multiple_circular_dependencies() {
        // o2 is part of 2 circular dependencies
        OrderableByNameImpl o1 = new OrderableByNameImpl("o1","o3");
        OrderableByNameImpl o2 = new OrderableByNameImpl("o2","o1, o2a");
        OrderableByNameImpl o3 = new OrderableByNameImpl("o3","o2");

        OrderableByNameImpl o2a = new OrderableByNameImpl("o2a","o2b");
        OrderableByNameImpl o2b = new OrderableByNameImpl("o2b","o2");

        try {
            orderableSorter.sort(list(o1, o2, o3, o2a, o2b));
        } catch (CircularDependencyException e) {
            assertEquals("OrderableByName 'o1' has a circular dependency: [o1 -> o3 -> o2 -> o1].", e.getMessage());
        }
    }

    /*
     * test missing dependency detection
     */

    @Test
    public void missing_dependency() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("o1","foo");

        try {
            orderableSorter.sort(list(o1));
        } catch (MissingDependencyException e) {
            assertEquals("OrderableByName 'o1' has a missing dependency 'foo'", e.getMessage());
        }
    }

    @Test
    public void missing_dependency_again() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("o1","o2");
        OrderableByNameImpl o2 = new OrderableByNameImpl("o2","foo");

        try {
            orderableSorter.sort(list(o1, o2));
        } catch (MissingDependencyException e) {
            assertEquals("OrderableByName 'o2' has a missing dependency 'foo'", e.getMessage());
        }
    }

    /*
     * test topological sorting
     */

    @Test
    public void sort_empty() {
        assertEquals("[]", sortedNames(list()));
    }

    @Test
    public void sort_one() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("a1");
        assertEquals("[a1]", sortedNames(list(o1)));
    }

    @Test
    public void sort_two_by_name() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("a1");
        OrderableByNameImpl o2 = new OrderableByNameImpl("a2");
        assertEquals("[a1, a2]", sortedNames(list(o2, o1)));
    }

    @Test
    public void sort_two_by_dependency() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("a1", "a2");
        OrderableByNameImpl o2 = new OrderableByNameImpl("a2");
        assertEquals("[a2, a1]", sortedNames(list(o1, o2)));
    }

    @Test
    public void sort_by_name() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("a1", "a3");
        OrderableByNameImpl o2 = new OrderableByNameImpl("a2");
        OrderableByNameImpl o3 = new OrderableByNameImpl("a3");
        assertEquals("[a3, a1, a2]", sortedNames(list(o1, o2, o3)));
        assertEquals("[a3, a1, a2]", sortedNames(list(o2, o1, o3)));
        assertEquals("[a3, a1, a2]", sortedNames(list(o3, o2, o1)));
    }

    @Test
    public void sort_by_dependency() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("a1", "a3, a2");
        OrderableByNameImpl o2 = new OrderableByNameImpl("a2");
        OrderableByNameImpl o3 = new OrderableByNameImpl("a3");

        assertEquals("[a2, a3, a1]", sortedNames(list(o1, o2, o3)));
    }

    @Test
    public void sort_by_name_and_dependency() {
        OrderableByNameImpl o1 = new OrderableByNameImpl("a1");
        OrderableByNameImpl o2 = new OrderableByNameImpl("b1", "b3");
        OrderableByNameImpl o3 = new OrderableByNameImpl("b2", "a1");
        OrderableByNameImpl o4 = new OrderableByNameImpl("b3", "a1");
        OrderableByNameImpl o5 = new OrderableByNameImpl("a2");
        OrderableByNameImpl o6 = new OrderableByNameImpl("b4", "b3");
        OrderableByNameImpl o7 = new OrderableByNameImpl("c1");

        assertEquals("[a1, a2, b3, b1, b2, b4, c1]", sortedNames(list(o1, o2, o3, o4, o5, o6, o7)));
    }

    private static List<OrderableByNameImpl> list(OrderableByNameImpl... args) {
        List<OrderableByNameImpl> result = new ArrayList<>();
        Collections.addAll(result, args);
        return result;
    }

    private static String sortedNames(List<OrderableByNameImpl> orderables) {
        orderableSorter.sort(orderables);
        return orderables.stream().map(OrderableByName::getName).collect(Collectors.toList()).toString();
    }
}
