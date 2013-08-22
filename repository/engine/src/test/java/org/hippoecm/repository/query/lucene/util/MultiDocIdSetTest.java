/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.query.lucene.util;

import java.io.IOException;
import java.util.Random;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultiDocIdSetTest {

    private static final int CHECK_INTERVAL = 200;
    private static final int NUM_BITSETS = 100;
    private static final int NUM_DOCS_IN_BITSET = 100;

    @Test
    public void testAdvance() throws IOException {
        Random rand = new Random(13);

        int[] maxDoc = new int[NUM_BITSETS];
        OpenBitSet[] bitsets = new OpenBitSet[NUM_BITSETS];
        for (int i = 0; i < NUM_BITSETS; i++) {
            OpenBitSet bitset = bitsets[i] = new OpenBitSet();
            for (int j = 0; j < NUM_DOCS_IN_BITSET; j++) {
                if (rand.nextInt(5) == 0) {
                    bitset.set(j);
                }
            }
            maxDoc[i] = NUM_DOCS_IN_BITSET;
        }
        int totalMaxDoc = NUM_BITSETS * NUM_DOCS_IN_BITSET;

        // compare nextDoc invocations with advance
        MultiDocIdSet docIdSet = new MultiDocIdSet(bitsets, maxDoc);
        final DocIdSetIterator simpleIterator = docIdSet.iterator();
        final DocIdSetIterator advancedIterator = docIdSet.iterator();

        int docId = 0;
        while (true) {
            final int delta = rand.nextInt(CHECK_INTERVAL);
            docId = docId + delta + 1;

            if (docId > totalMaxDoc) {
                break;
            }

            while (simpleIterator.docID() < docId && simpleIterator.nextDoc() != DocIdSetIterator.NO_MORE_DOCS);

            advancedIterator.advance(docId);

            assertEquals(simpleIterator.docID(), advancedIterator.docID());
        }
    }
}
