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
package org.onehippo.cm.engine.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import org.onehippo.cm.model.OrderableByName;
import org.onehippo.cm.model.impl.OrderableByNameListSorter;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.util.SnsUtils;

import com.google.common.collect.Sets;

/**
 * Sort definitions by natural order and order before so that independent definitions come first.
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

    public static class Item implements OrderableByName {

        private final ContentDefinitionImpl definition;

        public Item(ContentDefinitionImpl definition) {
            this.definition = definition;
        }

        @Override
        public String getName() {
            return SnsUtils.createIndexedName(definition.getNode().getName());
        }

        @Override
        public Set<String> getAfter() {
            return definition.getNode().getOrderBefore() == null ? Collections.emptySet()
                    : Sets.newHashSet(SnsUtils.createIndexedName(definition.getNode().getOrderBefore()));
        }

        public ContentDefinitionImpl getDefinition() {
            return definition;
        }

        @Override
        public String toString() {
            return "ContentDefinitionSorter.Item{" +
                    "name='" + getName() + '\'' +
                    ", before='" + definition.getNode().getOrderBefore() + '\'' +
                    ", definition=" + definition +
                    '}';
        }
    }

}
