/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.cms7.utilities.collections;

import java.util.Collection;
import java.util.LinkedList;

/**
 * A @{LinkedList} with a bounded capacity
 */
public class BoundedLinkedList<E> extends LinkedList<E> {
    private final long maxCapacity;

    public BoundedLinkedList(long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    @Override
    public boolean add(E element) {
        if (size() >= maxCapacity) {
             poll();
        }

        return super.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> colleciton) {
        boolean allAdded = true;

        for (E element : colleciton) {
            allAdded &= add(element);
        }

        return allAdded;
    }
}
