/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.content;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.onehippo.cms7.services.content.RangeIterable;

/**
 */
public class GenericRangeIterable<T> implements RangeIterable<T> {

    private final Iterator<T> iterator;
    private long size = -1;
    private long position;

    public GenericRangeIterable(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    public GenericRangeIterable(Iterator<T> iterator, long size) {
        this.iterator = iterator;
        this.size = size;
    }

    public GenericRangeIterable(Collection<T> collection) {
        this.iterator = collection.iterator();
        this.size = collection.size();
    }

    @Override
    public void skip(long n)
            throws IllegalArgumentException, NoSuchElementException {
        if (n < 0) {
            throw new IllegalArgumentException("skip(" + n + ")");
        }
        for (long i = 0; i < n; i++) {
            next();
        }
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        if (iterator.hasNext()) {
            return true;
        }
        else {
            if (size == -1) {
                size = position;
            }
            return false;
        }
    }

    @Override
    public T next() throws NoSuchElementException {
        try {
            T next = iterator.next();
            position++;
            return next;
        } catch (NoSuchElementException e) {
            if (size == -1) {
                size = position;
            }
            throw e;
        }
    }

    @Override
    public void remove()
            throws UnsupportedOperationException, IllegalStateException {
        iterator.remove();
        position--;
        if (size != -1) {
            size--;
        }
    }
}
