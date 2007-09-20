package org.hippoecm.repository.query.lucene;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
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

public class FacetResultCollector extends HitCollector {
    private IndexReader reader;
    private String internalName;
    private int numhits;
    private Set<String> hits;
    private Map<String,Count> facetMap;
    private FieldSelector fieldSelector; 
    private HitsRequested hitsRequested;
    private int offset;
    private int limit;
    
    public FacetResultCollector(IndexReader reader, String facet,  Map<String,Map<String,Count>> resultset, HitsRequested hitsRequested, NamespaceMappings nsMappings) {
        this.reader = reader;
        try {
         this.internalName = ServicingNameFormat.getInternalFacetName(facet, nsMappings);
         } catch(Exception ex) {
              System.err.println(ex.getMessage());
              ex.printStackTrace(System.err);
         }
         
        this.numhits = 0;
        this.hitsRequested = hitsRequested;
        
        Set<String> fieldNames = new HashSet<String>();
        // TODO UUID must become path
        fieldNames.add(FieldNames.UUID);
        this.fieldSelector = new SetBasedFieldSelector(fieldNames, new HashSet());
        
        if(hitsRequested.isResultRequested()) {
            this.hits = new HashSet<String>();
            this.offset = hitsRequested.getOffset();
            this.limit = hitsRequested.getLimit();
        } else {
            this.hits = null;
        }
        
        if(facet != null && resultset.get(facet)!= null) {
             facetMap = resultset.get(facet);
        }
    }
    public final void collect(final int docid, final float score) {
        try {
            if(hits != null) {
                if(offset == 0 && hits.size() < hitsRequested.getLimit() ) {
                    
                    Document d = reader.document(docid,fieldSelector);
                    // TODO UUID must become path
                    Field f = d.getField(FieldNames.UUID);
                    if(f!=null){
                        hits.add(f.stringValue());
                    }
                } else if (offset > 0){
                    // decrement offset untill it is 0. Then start gathering results above
                    offset--;
                }
            }
            
             final TermFreqVector tfv = reader.getTermFreqVector(docid, internalName);

             if(tfv != null) {
                 for(int i=0; i<tfv.getTermFrequencies().length; i++) {
                     Count count = facetMap.get(tfv.getTerms()[i]);
                     if(count == null) {
                         facetMap.put(tfv.getTerms()[i], new Count(1));
                     } else {
                         count.count += 1;
                      }
                 }
             }
             
            ++numhits;
        } catch(Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
    public final Set<String> getHits() {
        return hits;
    }
    public final int getNumhits() {
        return numhits;
    }
}


