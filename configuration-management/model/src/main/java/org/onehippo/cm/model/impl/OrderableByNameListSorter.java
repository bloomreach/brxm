/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cm.model.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.onehippo.cm.model.OrderableByName;
import org.onehippo.cm.model.impl.exceptions.CircularDependencyException;
import org.onehippo.cm.model.impl.exceptions.DuplicateNameException;
import org.onehippo.cm.model.impl.exceptions.MissingDependencyException;

/**
 * Topological <em>in place</em> {@link #sort(List) sorter} of a <em>modifiable</em> DAG list of {@link OrderableByName}s.
 * <p>
 * After sorting, the provided list will be cleared and filled again with the sorted result!
 * </p>
 * <p>
 * To guarantee a stable ordered result, independent of the ordering of the {@code OrderableByName}s in the list
 * and the ordering of their {@link OrderableByName#getAfter dependencies}, they all are processed in
 * alphabetically sorted order.
 * </p>
 * <p>
 * The {@link #sort(List)} provided list of {@code OrderableByName}s is automatically verified against duplicate
 * {@link OrderableByName#getName() named} elements and against cyclic or missing dependencies, and will throw a
 * {@link DuplicateNameException}, {@link CircularDependencyException} or {@link MissingDependencyException}
 * respectively when encountered.
 * </p>
 * <p>The {@link #OrderableByNameListSorter(Class)} constructor requires a {@code orderableType} parameter which
 * will be used for constructing the above mentioned exceptions error message.</p>
 * <p>
 * Usage:
 * <pre>
 *   private static final OrderableListSorter<GroupImpl> groupSorter = new OrderableListSorter<>(Group.class.getSimpleName());
 *   ...
 *   private List&lt;GroupImpl&gt; groups = new ArrayList&lt;&gt;();
 *   ...
 *   groupSorter.sort(groups);
 * </pre>
 * </p>
 * <p>
 * Implementation based on <a href="https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search">
 *     https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search</a>
 * </p>
 * @param <T> all objects sorted here are expected to be compatible with this type, and furthermore, {@link #sort(List)}
 *            is defined to operate on a List of uniform type, which may be any subtype of T
 */
public class OrderableByNameListSorter<T extends OrderableByName> {

    private final String orderableTypeName;

    public OrderableByNameListSorter(final Class<T> orderableType) {
        this.orderableTypeName = orderableType.getSimpleName();
    }

    public <U extends T> void sort(final List<U> orderables)
            throws DuplicateNameException, CircularDependencyException, MissingDependencyException {
        // using TreeMap ensures the orderables are processed in alphabetically sorted order
        final Map<String, U> map = new TreeMap<>(getComparator());

        for (U o : orderables) {
            if (map.containsValue(o)) {
                throw new DuplicateNameException(String.format("Duplicate %s named '%s'.", orderableTypeName, o));
            }
        }
        final LinkedHashMap<String, U> sorted = new LinkedHashMap<>(orderables.size());
        final List<String> dependencyChain = new ArrayList<>(sorted.size());
        for (U orderable : map.values()) {
            sortDepthFirst(orderable, dependencyChain, sorted, map);
        }

        orderables.clear();
        orderables.addAll(sorted.values());
    }

    protected Comparator<String> getComparator() {
        return Comparator.naturalOrder();
    }

    private <U extends T> void sortDepthFirst(final U orderable, final List<String> dependencyChain,
                                final LinkedHashMap<String, U> sorted, final Map<String, U> map)
            throws CircularDependencyException, MissingDependencyException {
        if (dependencyChain.contains(orderable.getName())) {
            dependencyChain.add(orderable.getName());
            throw new CircularDependencyException(String.format("%s '%s' has a circular dependency: [%s].",
                    orderableTypeName, dependencyChain.get(0),
                    dependencyChain.stream().collect(Collectors.joining(" -> "))));
        }
        if (!sorted.containsKey(orderable.getName())) {
            if (!orderable.getAfter().isEmpty()) {
                final List<String> dependencies = new ArrayList<>(orderable.getAfter());
                // ensure processing the dependencies in alphabetic order as well
                dependencies.sort(getComparator());
                dependencyChain.add(orderable.getName());
                for (String dependency : dependencies) {
                    if (map.containsKey(dependency)) {
                        sortDepthFirst(map.get(dependency), dependencyChain, sorted, map);
                    } else {
                        processMissingDependency(orderable, dependency);
                    }
                }
                dependencyChain.remove(orderable.getName());
            }
            sorted.put(orderable.getName(), orderable);
        }
    }

    protected <U extends T> void processMissingDependency(final U orderable, final String dependency) {
        throw new MissingDependencyException(String.format("%s '%s' has a missing dependency '%s'",
                orderableTypeName, orderable.getName(), dependency));
    }
}
