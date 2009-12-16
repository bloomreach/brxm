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

import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.HitCollector;
import org.hippoecm.repository.FacetedNavigationEngine.Count;
import org.hippoecm.repository.FacetedNavigationEngine.HitsRequested;

public class FacetResultCollector extends HitCollector {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private IndexReader reader;
    private String internalName;
    private int numhits;
    private Map<String, Count> facetMap;

    public FacetResultCollector(IndexReader reader, String facet, Map<String, Count> facetMap, HitsRequested hitsRequested) {
        this.reader = reader;
        if (facet != null) {
            try {
                this.internalName = ServicingNameFormat.getInternalFacetName(facet);
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        this.numhits = 0;
        this.facetMap = facetMap;
    }

    public final void collect(final int docid, final float score) {
        try {
            if (facetMap != null) {
                final TermFreqVector tfv = reader.getTermFreqVector(docid, internalName);
               
                if (tfv != null) {
                    numhits++;
                    for (int i = 0; i < tfv.getTermFrequencies().length; i++) {
                        Count count = facetMap.get(tfv.getTerms()[i]);
                        if (count == null) {
                            facetMap.put(tfv.getTerms()[i], new Count(1));
                        } else {
                            count.count += 1;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    public int getNumhits() {
        return numhits;
    }

    public void setNumhits(int numhits) {
        this.numhits = numhits;
    }
}
