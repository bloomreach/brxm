/*
 *  Copyright 2017-1018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cm.model.OrderableByName;
import org.onehippo.cm.model.impl.OrderableByNameListSorter;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.util.SnsUtils;

import com.google.common.collect.Sets;

import static org.onehippo.cm.model.Constants.META_ORDER_BEFORE_FIRST;

/**
 * Sort definitions by natural order and order before so that independent definitions come first.
 * Sorts definitions that have 'order-before first' as a separate group.
 * See also {@link OrderableByNameListSorter}
 */
public class ContentDefinitionSorter extends OrderableByNameListSorter<ContentDefinitionSorter.Item>{

    public ContentDefinitionSorter() {
        super(Item.class);
    }

    protected void processMissingDependency(final Item orderable, final String dependency) {
        //Do nothing
    }

    /**
     * Natural order comparator which also takes into account SNS names
     */
    protected Comparator<String> getComparator() {
        return (o1, o2) -> {
            final JcrPathSegment jcrPathSegment1 = JcrPaths.getSegment(o1);
            final JcrPathSegment jcrPathSegment2 = JcrPaths.getSegment(o2);
            return jcrPathSegment1.compareTo(jcrPathSegment2);
        };
    }

    @Override
    public <U extends ContentDefinitionSorter.Item> void sort(final List<U> orderables) {
        // Add an item representing 'first'; without this item, definitions that have 'order-before first' would
        // actually have a missing dependency.
        final ContentDefinitionSorter.Item first = new ContentDefinitionSorter.Item(orderables);
        orderables.add((U)first);
        super.sort(orderables);
        orderables.remove(first);
    }

    public static class Item implements OrderableByName {

        private final String name;
        private final String orderBefore;
        private final Set<String> after;
        private final ContentDefinitionImpl definition;

        public Item(ContentDefinitionImpl definition) {
            this.name = SnsUtils.createIndexedName(definition.getNode().getName());

            final String unindexedOrderBefore = definition.getNode().getOrderBefore();
            if (unindexedOrderBefore == null) {
                orderBefore = null;
                after = Collections.emptySet();
            } else if (unindexedOrderBefore.equals(META_ORDER_BEFORE_FIRST)) {
                orderBefore = META_ORDER_BEFORE_FIRST;
                after = Sets.newHashSet(META_ORDER_BEFORE_FIRST);
            } else {
                orderBefore = SnsUtils.createIndexedName(unindexedOrderBefore);
                after = Sets.newHashSet(orderBefore);
            }

            this.definition = definition;
        }

        /**
         * Special purpose constructor for creating an item representing 'first'.
         */
        private Item(final List<? extends Item> siblings) {
            name = META_ORDER_BEFORE_FIRST;
            orderBefore = null;
            after = new HashSet<>();
            definition = null;

            // 'first' must be ordered after siblings that have no order-before and (recursively) their dependencies
            buildAfter(null, siblings, after);
        }

        private void buildAfter(final String orderBefore, final List<? extends Item> siblings, final Set<String> after) {
            for (final Item item : siblings) {
                if (StringUtils.equals(orderBefore, item.getOrderBefore())) {
                    after.add(item.getName());
                    buildAfter(item.getName(), siblings, after);
                }
            }
        }

        @Override
        public String getName() {
            return name;
        }

        public String getOrderBefore() {
            return orderBefore;
        }

        @Override
        public Set<String> getAfter() {
            return after;
        }

        public ContentDefinitionImpl getDefinition() {
            return definition;
        }

        @Override
        public String toString() {
            return "ContentDefinitionSorter.Item{" +
                    "name='" + name + '\'' +
                    ", orderBefore='" + orderBefore + '\'' +
                    ", after=" + after.toString() +
                    ", definition=" + definition +
                    '}';
        }
    }

}
