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
package org.onehippo.cm.impl.model.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.impl.model.builder.exceptions.CircularDependencyException;
import org.onehippo.cm.impl.model.builder.exceptions.DuplicateNameException;
import org.onehippo.cm.impl.model.builder.exceptions.MissingDependencyException;

/**
 * Topological <em>in place</em> {@link #sort(List) sorter} of a <em>modifiable</em> DAG list of {@link Orderable}s.
 * <p>
 * After sorting the provided list will be cleared and filled again with the sorted result!
 * </p>
 * <p>
 * To guarantee a stable ordered result, independent of the ordering of the {@code Orderable}s in the list
 * and the ordering of their {@link Orderable#getAfter dependencies}, they all are processed in
 * alphabetically sorted order.
 * </p>
 * <p>
 * The {@link #sort(List)} provided list of {@code Orderable}s is automatically verified against duplicate
 * {@link Orderable#getName() named} elements and against cyclic or missing dependencies, and will throw a
 * {@link DuplicateNameException}, {@link CircularDependencyException} or {@link MissingDependencyException}
 * respectively when encountered.
 * </p>
 * <p>The {@link #OrderableListSorter(String)} constructor requires a {@code orderableTypeName} parameter which
 * will be used for constructing the above mentioned exceptions error message.</p>
 * <p>
 * Usage:
 * <pre>
 *   private static final OrderableListSorter<ConfigurationImpl> configurationSorter = new OrderableListSorter<>(Configuration.class.getSimpleName());
 *   ...
 *   private List&lt;ConfigurationImpl&gt; configurations = new ArrayList&lt;&gt;();
 *   ...
 *   configurationSorter.sort(configurations);
 * </pre>
 * </p>
 * <p>
 * Implementation based on <a href="https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search">
 *     https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search</a>
 * </p>
 */
public class OrderableListSorter<T extends Orderable> {

    private final String orderableTypeName;

    public OrderableListSorter(final String orderableTypeName) {
        this.orderableTypeName = orderableTypeName;
    }

    public void sort(final List<T> orderables)
            throws DuplicateNameException, CircularDependencyException, MissingDependencyException {
        // using TreeMap ensures the orderables are processed in alphabetically sorted order
        Map<String, T> map = new TreeMap<>();
        for (T o : orderables) {
            if (map.put(o.getName(), o) != null) {
                throw new DuplicateNameException(String.format("Duplicate %s named '%s'.", orderableTypeName, o.getName()));
            }
        }
        LinkedHashMap<String, T> sorted = new LinkedHashMap<>(orderables.size());
        List<String> dependencyChain = new ArrayList<>(sorted.size());
        for (T orderable : map.values()) {
            sortDeptFirst(orderable, dependencyChain, sorted, map);
        }

        orderables.clear();
        orderables.addAll(sorted.values());
    }

    private void sortDeptFirst(final T orderable, final List<String> dependencyChain,
                               final LinkedHashMap<String, T> sorted, final Map<String, T> map)
            throws CircularDependencyException, MissingDependencyException {
        if (dependencyChain.contains(orderable.getName())) {
            dependencyChain.add(orderable.getName());
            throw new CircularDependencyException(String.format("%s '%s' has a circular dependency: [%s].",
                    orderableTypeName, dependencyChain.get(0),
                    dependencyChain.stream().collect(Collectors.joining(" -> "))));
        }
        if (!sorted.containsKey(orderable.getName())) {
            if (!orderable.getAfter().isEmpty()) {
                List<String> dependencies = new ArrayList<>(orderable.getAfter());
                // ensure processing the dependencies in alphabetic order as well
                Collections.sort(dependencies);
                dependencyChain.add(orderable.getName());
                for (String dependency : dependencies) {
                    if (map.containsKey(dependency)) {
                        sortDeptFirst(map.get(dependency), dependencyChain, sorted, map);
                    } else {
                        throw new MissingDependencyException(String.format("%s '%s' has a missing dependency '%s'",
                                orderableTypeName, orderable.getName(), dependency));
                    }
                }
                dependencyChain.remove(orderable.getName());
            }
            sorted.put(orderable.getName(), orderable);
        }
    }
}
