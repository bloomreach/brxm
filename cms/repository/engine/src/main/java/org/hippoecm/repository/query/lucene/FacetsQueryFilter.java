/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.query.lucene;

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.hippoecm.repository.query.lucene.caching.CachedQueryBitSet;

public class FacetsQueryFilter extends Filter {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private BitSet bits;

    public FacetsQueryFilter(BitSet authorized) {
        this.bits = authorized;
    }

    public FacetsQueryFilter(IndexReader indexReader, BooleanQuery query,
            Map<String, CachedQueryBitSet> cachedQueryBitSetsMap) {
        // use cachedQueryBitSet
    }

    @Override
    public BitSet bits(IndexReader in) throws IOException {
        return this.bits;
    }
}
