package org.hippoecm.repository.query.lucene;

import java.io.IOException;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;

public class ParallelMultiSearcher extends org.apache.lucene.search.ParallelMultiSearcher{
    private Searchable[] searchables;
    private int[] starts;
    
    public ParallelMultiSearcher(Searchable[] searchables) throws IOException {
        super(searchables);
        this.searchables=searchables;
        this.starts=getStarts();
    }

    /**
     * 
     * @TODO parallelize and cache
     */
   public void search(Weight weight, Filter filter, final HitCollector results)
     throws IOException {
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
