package org.hippoecm.repository.query.lucene.caching;

import java.util.BitSet;

import org.apache.lucene.index.IndexReader;

public class CachedQueryBitSet {
    private BitSet bitSet = null;
    private int maxDoc = 0;
    
    public CachedQueryBitSet(BitSet bitSet, int maxDoc) {
        this.bitSet = bitSet;
        this.maxDoc = maxDoc;
    }

    public BitSet getBitSet() {
        return bitSet;
    }
    
    public int getMaxDoc() {
        return maxDoc;
    }

    public boolean isValid(IndexReader indexReader) {
        // TODO check on creationtick per index reader
        if(this.maxDoc == indexReader.maxDoc()) {
            // dummy check for now for validity: needs enhancement
            return true;
        } 
        return false;
    }


}
