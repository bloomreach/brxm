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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.HitCollector;
import org.hippoecm.repository.HitsRequested;
import org.hippoecm.repository.FacetedNavigationEngine.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class is unused since faceted navigation caching. Kept for now because it might be used later again for more efficient range queries
 * on Double's and Long
 * @deprecated
 */
public class FacetResultCollector extends HitCollector {

    private static final Logger log = LoggerFactory.getLogger(FacetResultCollector.class);
    private IndexReader reader;
    private String internalName;
    private int numhits;
    private Map<String, Count> facetMap;
    private List<String[]> facetRangeList;

    public FacetResultCollector(IndexReader reader, String propertyName, Map<String, Count> facetMap, List<String[]> facetRangeList, HitsRequested hitsRequested) {
        this.reader = reader;
        if (propertyName != null) {
            try {
                this.internalName = ServicingNameFormat.getInternalFacetName(propertyName);
            } catch (Exception ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            }
        }
        this.numhits = 0;
        this.facetMap = facetMap;
        if(facetRangeList != null) {
            this.facetRangeList = validateRanges(facetRangeList);
        }
    }

    

    public final void collect(final int docid, final float score) {
        try {
            if (facetMap != null) {
                final TermFreqVector tfv = reader.getTermFreqVector(docid, internalName);
               
                if (tfv != null) {
                    numhits++;
                    for (int i = 0; i < tfv.getTermFrequencies().length; i++) {
                        if(facetRangeList == null) {
                            addToFacetMap(tfv.getTerms()[i]);
                        } else {
                            String term = tfv.getTerms()[i];
                            for(String[] facetRange : facetRangeList){
                                // items are already validated in construcutor, so no checks for length of array or null checks
                                if(term.compareTo(facetRange[1]) >= 0 && term.compareTo(facetRange[2]) < 0) {
                                    // add the facet name to the resultset
                                    addToFacetMap(facetRange[0]);
                                }  
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
    }

    private void addToFacetMap(String term) {
        Count count = facetMap.get(term);
        if (count == null) {
            facetMap.put(term, new Count(1));
        } else {
            count.count += 1;
        }
    }

    public int getNumhits() {
        return numhits;
    }

    public void setNumhits(int numhits) {
        this.numhits = numhits;
    }
    
    private List<String[]> validateRanges(List<String[]> ranges2validate) {
        List<String[]> validatedList = new ArrayList<String[]>();
        for(String[] facetRange : ranges2validate) {
            if(facetRange.length == 3 && facetRange[0] != null && facetRange[1] != null && facetRange[2] != null) {
                validatedList.add(facetRange);
            } else {
                log.error("Invalid range found: '{}'. Skipping this range", facetRange);
            }
        }
        return validatedList;
    }
}
