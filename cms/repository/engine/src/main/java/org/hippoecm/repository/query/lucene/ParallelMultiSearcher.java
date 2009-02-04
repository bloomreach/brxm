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

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.Weight;

public class ParallelMultiSearcher extends org.apache.lucene.search.ParallelMultiSearcher {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private Searchable[] searchables;
    private int[] starts;

    public ParallelMultiSearcher(Searchable[] searchables) throws IOException {
        super(searchables);
        this.searchables = searchables;
        this.starts = getStarts();
    }

    /**
     *
     * @TODO parallelize and cache
     */
    public void search(Weight weight, Filter filter, final HitCollector results) throws IOException {
        for (int i = 0; i < searchables.length; i++) {
            final int start = starts[i];
            searchables[i].search(weight, filter, new HitCollector() {
                public void collect(int doc, float score) {
                    results.collect(doc + start, score);
                }
            });
        }
    }
}
