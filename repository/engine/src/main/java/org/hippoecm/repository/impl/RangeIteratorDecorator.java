/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import javax.jcr.RangeIterator;

public class RangeIteratorDecorator extends SessionBoundDecorator implements RangeIterator {

    protected final RangeIterator iterator;

    RangeIteratorDecorator(final SessionDecorator session, final RangeIterator iterator) {
        super(session);
        this.iterator = iterator;
    }

    public void skip(long skipNum) {
        iterator.skip(skipNum);
    }

    public long getSize() {
        return iterator.getSize();
    }

    public long getPosition() {
        return iterator.getPosition();
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public ItemDecorator next() {
        return ItemDecorator.newItemDecorator(session, iterator.next());
    }

    public void remove() {
        iterator.remove();
    }

}
