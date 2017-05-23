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
package org.onehippo.cm.impl.model.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.impl.model.OrderableImpl;
import org.onehippo.cm.impl.model.builder.exceptions.CircularDependencyException;
import org.onehippo.cm.impl.model.builder.exceptions.DuplicateNameException;
import org.onehippo.cm.impl.model.builder.exceptions.MissingDependencyException;

import static org.junit.Assert.assertEquals;

public class OrderableListSorterTest {

    private static final OrderableListSorter<OrderableImpl> orderableSorter = new OrderableListSorter<>(Orderable.class.getSimpleName());

    /*
     * test duplicate name detection
     */

    @Test
    public void duplicate_names() {
        OrderableImpl o1 = new OrderableImpl("o1", "");
        OrderableImpl o2 = new OrderableImpl("o1", "");

        try {
            orderableSorter.sort(list(o1, o2));
        } catch (DuplicateNameException e) {
            assertEquals("Duplicate Orderable named 'o1'.", e.getMessage());
        }
    }

    /*
     * test circular dependency detection
     */

    @Test
    public void self_circular_dependency() {
        OrderableImpl o1 = new OrderableImpl("o1", "o1");
        OrderableImpl o2 = new OrderableImpl("o2", "");

        try {
            orderableSorter.sort(list(o1, o2));
        } catch (CircularDependencyException e) {
            assertEquals("Orderable 'o1' has a circular dependency: [o1 -> o1].", e.getMessage());
        }
    }

    @Test
    public void two_wise_circular_dependency() {
        OrderableImpl o1 = new OrderableImpl("o1","o2");
        OrderableImpl o2 = new OrderableImpl("o2","o1");

        try {
            orderableSorter.sort(list(o1, o2));
        } catch (CircularDependencyException e) {
            assertEquals("Orderable 'o1' has a circular dependency: [o1 -> o2 -> o1].", e.getMessage());
        }
    }

    @Test
    public void three_wise_circular_dependency() {
        OrderableImpl o1 = new OrderableImpl("o1","o2");
        OrderableImpl o2 = new OrderableImpl("o2","o3");
        OrderableImpl o3 = new OrderableImpl("o3","o1");

        try {
            orderableSorter.sort(list(o1, o2, o3));
        } catch (CircularDependencyException e) {
            assertEquals("Orderable 'o1' has a circular dependency: [o1 -> o2 -> o3 -> o1].", e.getMessage());
        }
    }

    @Test
    public void complex_multiple_circular_dependencies() {
        // o2 is part of 2 circular dependencies
        OrderableImpl o1 = new OrderableImpl("o1","o3");
        OrderableImpl o2 = new OrderableImpl("o2","o1, o2a");
        OrderableImpl o3 = new OrderableImpl("o3","o2");

        OrderableImpl o2a = new OrderableImpl("o2a","o2b");
        OrderableImpl o2b = new OrderableImpl("o2b","o2");

        try {
            orderableSorter.sort(list(o1, o2, o3, o2a, o2b));
        } catch (CircularDependencyException e) {
            assertEquals("Orderable 'o1' has a circular dependency: [o1 -> o3 -> o2 -> o1].", e.getMessage());
        }
    }

    /*
     * test missing dependency detection
     */

    @Test
    public void missing_dependency() {
        OrderableImpl o1 = new OrderableImpl("o1","foo");

        try {
            orderableSorter.sort(list(o1));
        } catch (MissingDependencyException e) {
            assertEquals("Orderable 'o1' has a missing dependency 'foo'", e.getMessage());
        }
    }

    @Test
    public void missing_dependency_again() {
        OrderableImpl o1 = new OrderableImpl("o1","o2");
        OrderableImpl o2 = new OrderableImpl("o2","foo");

        try {
            orderableSorter.sort(list(o1, o2));
        } catch (MissingDependencyException e) {
            assertEquals("Orderable 'o2' has a missing dependency 'foo'", e.getMessage());
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
        OrderableImpl o1 = new OrderableImpl("a1");
        assertEquals("[a1]", sortedNames(list(o1)));
    }

    @Test
    public void sort_two_by_name() {
        OrderableImpl o1 = new OrderableImpl("a1");
        OrderableImpl o2 = new OrderableImpl("a2");
        assertEquals("[a1, a2]", sortedNames(list(o2, o1)));
    }

    @Test
    public void sort_two_by_dependency() {
        OrderableImpl o1 = new OrderableImpl("a1", "a2");
        OrderableImpl o2 = new OrderableImpl("a2");
        assertEquals("[a2, a1]", sortedNames(list(o1, o2)));
    }

    @Test
    public void sort_by_name() {
        OrderableImpl o1 = new OrderableImpl("a1", "a3");
        OrderableImpl o2 = new OrderableImpl("a2");
        OrderableImpl o3 = new OrderableImpl("a3");
        assertEquals("[a3, a1, a2]", sortedNames(list(o1, o2, o3)));
        assertEquals("[a3, a1, a2]", sortedNames(list(o2, o1, o3)));
        assertEquals("[a3, a1, a2]", sortedNames(list(o3, o2, o1)));
    }

    @Test
    public void sort_by_dependency() {
        OrderableImpl o1 = new OrderableImpl("a1", "a3, a2");
        OrderableImpl o2 = new OrderableImpl("a2");
        OrderableImpl o3 = new OrderableImpl("a3");

        assertEquals("[a2, a3, a1]", sortedNames(list(o1, o2, o3)));
    }

    @Test
    public void sort_by_name_and_dependency() {
        OrderableImpl o1 = new OrderableImpl("a1");
        OrderableImpl o2 = new OrderableImpl("b1", "b3");
        OrderableImpl o3 = new OrderableImpl("b2", "a1");
        OrderableImpl o4 = new OrderableImpl("b3", "a1");
        OrderableImpl o5 = new OrderableImpl("a2");
        OrderableImpl o6 = new OrderableImpl("b4", "b3");
        OrderableImpl o7 = new OrderableImpl("c1");

        assertEquals("[a1, a2, b3, b1, b2, b4, c1]", sortedNames(list(o1, o2, o3, o4, o5, o6, o7)));
    }

    private static List<OrderableImpl> list(OrderableImpl... args) {
        List<OrderableImpl> result = new ArrayList<>();
        Collections.addAll(result, args);
        return result;
    }

    private static String sortedNames(List<OrderableImpl> orderables) {
        orderableSorter.sort(orderables);
        return orderables.stream().map(Orderable::getName).collect(Collectors.toList()).toString();
    }
}
