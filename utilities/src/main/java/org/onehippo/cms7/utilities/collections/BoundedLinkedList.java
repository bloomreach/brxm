/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
    private final int maxCapacity;

    public BoundedLinkedList(int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Maximum capacity must be greater than 0");
        }

        this.maxCapacity = maxCapacity;
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    @Override
    public boolean add(E element) {
        if (size() >= maxCapacity) {
             poll();
        }

        return super.add(element);
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        boolean changed = false;

        for (E element : collection) {
            changed |= add(element);
        }

        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFirst(E element) {
        validateCapacityAndPollFirstIfNeeded();
        super.addFirst(element);
    }

    @Override
    public void addLast(E element) {
        validateCapacityAndPollFirstIfNeeded();
        super.addLast(element);
    }

    @Override
    public boolean offer(E element) {
        validateCapacityAndPollFirstIfNeeded();
        return super.offer(element);
    }

    @Override
    public boolean offerFirst(E element) {
        validateCapacityAndPollFirstIfNeeded();
        return super.offerFirst(element);
    }

    @Override
    public boolean offerLast(E element) {
        validateCapacityAndPollFirstIfNeeded();
        return super.offerLast(element);
    }

    @Override
    public void push(E element) {
        validateCapacityAndPollFirstIfNeeded();
        super.push(element);
    }

    protected void validateCapacityAndPollFirstIfNeeded() {
        if (size() >= maxCapacity) {
            poll();
        }
    }

}
