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
package org.hippoecm.hst.solr.content.beans.query;

import java.io.Serializable;
import java.util.Iterator;

public interface HitIterator extends Iterator<Hit>, Serializable {

    /**
     * Skip a number of elements in the iterator. Using skip is very efficient when you know you want to
     * have the, say, 100 to 110 beans, as the skip does not need to fetch number 1 to 100.
     *
     * @param skipNum the non-negative number of elements to skip
     * @throws java.util.NoSuchElementException
     *          if skipped past the last element in the iterator.
     */
    void skip(int skipNum);

    /**
     * Returns the number of elements in the iterator. If this information is unavailable, returns -1.
     *
     * @return a long
     */
    long getSize();

    /**
     * Returns the current position within the iterator. The number returned is the 0-based index of the
     * next element in the iterator, i.e. the one that will be returned on the subsequent <code>next</code>
     * call.
     * <p/>
     * Note that this method does not check if there is a next element, i.e. an empty iterator will always
     * return 0.
     *
     * @return a long
     */
    long getPosition();
}
