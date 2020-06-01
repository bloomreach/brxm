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
package org.hippoecm.repository.util;

import java.util.Iterator;

import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

public class RowIterable implements Iterable<Row> {
    private final RowIterator iterator;

    public RowIterable(RowIterator iterator) {
        this.iterator = iterator;
    }

    @SuppressWarnings("unchecked")
    public Iterator<Row> iterator() {
        return iterator;
    }
}
