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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.SetBasedFieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.HitCollector;
import org.hippoecm.repository.HitsRequested;
import org.hippoecm.repository.FacetedNavigationEngine.Count;

public class CachingFacetResultCollector extends HitCollector {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private IndexReader reader;
    private String internalName;
    private int numhits;
    private Set<String> hits;
    private Map<String, Count> facetMap;
    private FieldSelector fieldSelector;
    private int offset;
    private int limit;
    private Map<String, Map<Integer, String[]>> collectorTFVMap;
    private Map<Integer, String[]> internalNameTermsMap;
    private boolean checkCache = true;

    public CachingFacetResultCollector(IndexReader reader,
                                       Map<IndexReader, Map<String, Map<Integer, String[]>>> tfvCache,
                                       String facet, Map<String, Map<String, Count>> resultset,
                                       HitsRequested hitsRequested, NamespaceMappings nsMappings) {
        this.reader = reader;

        try {
            this.internalName = ServicingNameFormat.getInternalFacetName(facet);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }

        this.collectorTFVMap = tfvCache.get(reader);
        if (collectorTFVMap == null) {
            collectorTFVMap = new HashMap<String, Map<Integer, String[]>>();
            tfvCache.put(reader, collectorTFVMap);
        }

        this.internalNameTermsMap = collectorTFVMap.get(internalName);
        if (internalNameTermsMap == null) {
            checkCache = false;
            this.internalNameTermsMap = new HashMap<Integer, String[]>();
            collectorTFVMap.put(internalName, internalNameTermsMap);
        }
        this.numhits = 0;

        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add(ServicingFieldNames.HIPPO_PATH);
        this.fieldSelector = new SetBasedFieldSelector(fieldNames, new HashSet());

        if (hitsRequested.isResultRequested()) {
            this.hits = new HashSet<String>();
            this.offset = hitsRequested.getOffset();
            this.limit = hitsRequested.getLimit();
        } else {
            this.hits = null;
        }

        if (facet != null && resultset.get(facet) != null) {
            facetMap = resultset.get(facet);
        }
    }

    public final void collect(final int docid, final float score) {
        try {
            if (hits != null) {
                if (offset == 0 && hits.size() < limit) {
                    Document d = reader.document(docid, fieldSelector);
                    Field f = d.getField(ServicingFieldNames.HIPPO_PATH);
                    if (f != null) {
                        hits.add(f.stringValue());
                    }
                } else if (offset > 0) {
                    // decrement offset untill it is 0. Then start gathering results above
                    offset--;
                }
            }

            String[] terms = null;
            // if checkCache is true, first check the cache. This is faster
            if (checkCache) {
                terms = internalNameTermsMap.get(docid);
                if (terms == null) {
                    terms = getFacetTerms(reader, docid, internalName, internalNameTermsMap);
                }
            } else {
                terms = getFacetTerms(reader, docid, internalName, internalNameTermsMap);
            }

            if (terms != null) {
                for (int i = 0; i < terms.length; i++) {
                    Count count = facetMap.get(terms[i]);
                    if (count == null) {
                        facetMap.put(terms[i], new Count(1));
                    } else {
                        count.count += 1;
                    }
                }
            }

            ++numhits;
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    private String[] getFacetTerms(IndexReader reader, int docid, String internalName, Map<Integer, String[]> internalNameTermsMap) throws IOException {
        // TODO improve memory useage! Storing int/bytes instead of terms (references to String pool)
        // and hold a list of {int,term} mappings to translate later
        TermFreqVector tfv = reader.getTermFreqVector(docid, internalName);
        String[] terms = new String[tfv.getTermFrequencies().length];
        for (int i = 0; i < tfv.getTermFrequencies().length; i++) {
            terms[i] = tfv.getTerms()[i];
        }
        internalNameTermsMap.put(docid, terms);
        return terms;
    }

    public final Set<String> getHits() {
        return hits;
    }

    public final int getNumhits() {
        return numhits;
    }
}


