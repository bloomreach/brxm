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
package org.onehippo.cm.impl.model.builder.sorting;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.impl.model.OrderableImpl;

import static org.junit.Assert.assertEquals;

public class OrderableComparatorTest {

    @Test
    public void sort_two_orderables() {
        // o1 depends on o2
        Orderable o1 = new OrderableImpl("o1", "o2");
        Orderable o2 = new OrderableImpl("o2");

        assertEquals("[o2, o1]", names(ordered(Arrays.asList(o1, o2))));
        assertEquals("[o2, o1]", names(ordered(Arrays.asList(o2, o1))));
    }

    @Test
    public void sort_three_orderables() {
        // o1 depends on o2, and o2 depends on o3
        Orderable o1 = new OrderableImpl("o1", "o2");
        Orderable o2 = new OrderableImpl("o2", "o3");
        Orderable o3 = new OrderableImpl("o3");

        assertEquals("[o3, o2, o1]", names(ordered(Arrays.asList(o1, o2, o3))));
        assertEquals("[o3, o2, o1]", names(ordered(Arrays.asList(o1, o3, o2))));
        assertEquals("[o3, o2, o1]", names(ordered(Arrays.asList(o2, o1, o3))));
        assertEquals("[o3, o2, o1]", names(ordered(Arrays.asList(o2, o3, o1))));
        assertEquals("[o3, o2, o1]", names(ordered(Arrays.asList(o3, o1, o2))));
        assertEquals("[o3, o2, o1]", names(ordered(Arrays.asList(o3, o2, o1))));
    }

    @Test
    public void sort_three_orderables_with_multiple_dependenies() {
        // o1 depends on o2, an o3 depends on both o1 and o2
        Orderable o1 = new OrderableImpl("o1", "o2");
        Orderable o2 = new OrderableImpl("o2");
        Orderable o3 = new OrderableImpl("o3", "o1, o2");

        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o1, o2, o3))));
        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o1, o3, o2))));
        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o2, o1, o3))));
        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o2, o3, o1))));
        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o3, o1, o2))));
        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o3, o2, o1))));
    }

    @Test
    public void resort_to_name_when_after_ordering_is_undeterministic() {
        // o1 and o3 depend both on o2, but there is no explicit ordering between o1 and o3
        Orderable o1 = new OrderableImpl("o1", "o2");
        Orderable o2 = new OrderableImpl("o2");
        Orderable o3 = new OrderableImpl("o3", "o2");

        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o1, o2, o3))));
        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o1, o3, o2))));
        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o2, o1, o3))));
        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o2, o3, o1))));
        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o3, o1, o2))));
        assertEquals("[o2, o1, o3]", names(ordered(Arrays.asList(o3, o2, o1))));
    }

    private List<Orderable> ordered(List<Orderable> orderables) {
        orderables.sort(new OrderableComparator<>());
        return orderables;
    }

    private String names(List<Orderable> ordered) {
        return ordered.stream().map(Orderable::getName).collect(Collectors.toList()).toString();
    }
}
