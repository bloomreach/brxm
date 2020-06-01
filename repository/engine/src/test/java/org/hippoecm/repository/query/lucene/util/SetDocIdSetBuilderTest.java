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

import org.apache.lucene.util.OpenBitSet;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SetDocIdSetBuilderTest {

    private static final int BITSETSIZE = 10 * 1000 * 1000;

    @Test
    public void testCombineBigDocIdSets() throws IOException {
        Random rand = new Random();
        int nBitsets = 10;
        OpenBitSet[] bitSets = new OpenBitSet[nBitsets];
        for (int i = 0; i < nBitsets; i++) {
            OpenBitSet bitSet = new OpenBitSet();
            for (int j = 0; j < BITSETSIZE; j++) {
                if (rand.nextInt(i + 1) == 0) {
                    bitSet.set(j);
                }
            }
            bitSets[i] = bitSet;
        }

        long builtCardinality;
        {
            long start = System.currentTimeMillis();
            SetDocIdSetBuilder builder = new SetDocIdSetBuilder();
            for (int i = 0; i < 10; i++) {
                builder.add(bitSets[i]);
            }
            final OpenBitSet result = builder.toBitSet();
            builtCardinality = result.cardinality();
//            System.out.println("docidsetbuilder time: " + (System.currentTimeMillis() - start) + ", cardinality: " + builtCardinality);
        }

        /*
        {
            long start = System.currentTimeMillis();
            OpenBitSet bitSet = (OpenBitSet) bitSets[0].clone();
            for (int i = 1; i < 10; i++) {
                OpenBitSet clone = new OpenBitSet();
                final DocIdSetIterator iterator = bitSets[i].iterator();
                while (true) {
                    iterator.nextDoc();
                    int docId = iterator.docID();
                    if (docId == DocIdSetIterator.NO_MORE_DOCS) {
                        break;
                    }
                    clone.set(docId);
                }
                bitSet.and(clone);
            }
            System.out.println("to bitset + bitset#and time: " + (System.currentTimeMillis() - start));
        }
        */

        long expectedCardinality;
        {
            long start = System.currentTimeMillis();
            OpenBitSet bitSet =  (OpenBitSet)bitSets[0].clone();
            for (int i = 1; i < 10; i++) {
                bitSet.and(bitSets[i]);
            }
            expectedCardinality = bitSet.cardinality();
//            System.out.println("cardinality: " + expectedCardinality + ", pure bitset time: " + (System.currentTimeMillis() - start));
        }

        assertEquals(expectedCardinality, builtCardinality);
    }
}
