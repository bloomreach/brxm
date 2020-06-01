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
package org.onehippo.repository.mock;

import java.util.Collection;
import java.util.Iterator;

import javax.jcr.RangeIterator;

public class MockItemIterator<T> implements RangeIterator {

    private final Iterator<T> iterator;
    private final int size;

    public MockItemIterator(Collection<T> collection) {
        iterator = collection.iterator();
        size = collection.size();
    }

    @Override
    public void skip(final long skipNum) {
        for (int i = 0; i < skipNum; i++) {
            next();
        }
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public long getPosition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
