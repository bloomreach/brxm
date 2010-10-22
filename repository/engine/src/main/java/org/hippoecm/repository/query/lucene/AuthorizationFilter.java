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
import org.apache.lucene.search.QueryWrapperFilter;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.query.lucene.caching.CachedAuthorizationBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationFilter extends Filter {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(FacetedNavigationEngine.class);

    private BitSet authorized;

    public AuthorizationFilter(BitSet authorized) {
        this.authorized = authorized;
    }

    public AuthorizationFilter(IndexReader indexReader, AuthorizationQuery authorizationQuery,
                               Map<String, CachedAuthorizationBitSet> cachedAuthorizationBitSetsMap) throws IOException {
        BooleanQuery authQuery = authorizationQuery.getQuery();
        if (authQuery == null) {
            // should never happen: add bitset of all 0
            authorized = new BitSet(indexReader.maxDoc());
        } else if (authQuery.clauses().size() > 0) {
            String cachekey = authQuery.toString();
            CachedAuthorizationBitSet cabs = cachedAuthorizationBitSetsMap.get(cachekey);
            if (cabs == null || !cabs.isValid(indexReader)) {
                long start = System.currentTimeMillis();
                Filter authFilter = new QueryWrapperFilter(authQuery);
                BitSet bits = authFilter.bits(indexReader);
                cachedAuthorizationBitSetsMap.put(cachekey, new CachedAuthorizationBitSet(bits, indexReader.maxDoc()));
                authorized = bits;
                log.debug("authorization BitSet creation took: " + (System.currentTimeMillis() - start) + " ms for query : " + authQuery.toString());
            } else {
                log.debug("Valid cached authorization BitSet found");
                authorized = cachedAuthorizationBitSetsMap.get(cachekey).getBitSet();
            }
        } else {
            // user is allowed to see everything: add bitset of all 1
            BitSet bits = new BitSet(indexReader.maxDoc());
            bits.flip(0, indexReader.maxDoc());
            authorized = bits;
        }

    }

    @Override
    public BitSet bits(IndexReader in) throws IOException {
        return authorized;
    }
}
