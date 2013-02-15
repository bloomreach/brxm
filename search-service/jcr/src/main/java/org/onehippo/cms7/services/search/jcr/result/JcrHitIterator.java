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
package org.onehippo.cms7.services.search.jcr.result;

import java.util.Set;

import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.onehippo.cms7.services.search.result.Hit;
import org.onehippo.cms7.services.search.result.HitIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrHitIterator implements HitIterator {

    static final Logger log = LoggerFactory.getLogger(JcrHitIterator.class);

    private final RowIterator rowIterator;
    private final Set<String> columnNames;

    public JcrHitIterator(final RowIterator rowIterator, Set<String> columnNames) {
        this.rowIterator = rowIterator;
        this.columnNames = columnNames;
    }

    @Override
    public void skip(final int skipCount) {
        rowIterator.skip(skipCount);
    }

    @Override
    public Hit nextHit() {
        final Row row = rowIterator.nextRow();
        return new JcrHit(row, columnNames);
    }

    @Override
    public boolean hasNext() {
        return rowIterator.hasNext();
    }

    @Override
    public Hit next() {
        return nextHit();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
