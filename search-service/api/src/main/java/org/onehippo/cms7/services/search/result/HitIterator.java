/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.result;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface HitIterator extends Iterator<Hit> {

    public HitIterator EMPTY = new HitIterator() {
        @Override
        public void skip(final int skipCount) {
        }

        @Override
        public Hit nextHit() {
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Hit next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    };

    /**
     * Skip a number of elements in the iterator. Using skip is very efficient when you know you want to have the, say, 100 to 110 
     * beans, as the skip does not need to fetch number 1 to 100. 
     * @param skipCount the non-negative number of elements to skip
     */
    void skip(int skipCount);
 
    /**
     * Returns the next content in the iteration.
     * @return
     */
    public Hit nextHit();

}
