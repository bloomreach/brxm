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
package org.hippoecm.repository.query.lucene.caching;

import java.util.BitSet;

import org.apache.lucene.index.IndexReader;

public class CachedQueryBitSet {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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
        if (this.maxDoc == indexReader.maxDoc()) {
            // dummy check for now for validity: needs enhancement
            return true;
        }
        return false;
    }
}
