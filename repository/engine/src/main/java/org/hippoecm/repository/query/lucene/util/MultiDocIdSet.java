/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

public class MultiDocIdSet extends DocIdSet {

    private final int[] maxDocs;
    private final DocIdSet[] docIdSets;

    public MultiDocIdSet(final DocIdSet[] docIdSets, final int[] maxDocs) {
        this.docIdSets = docIdSets;
        this.maxDocs = maxDocs;
    }

    @Override
    public DocIdSetIterator iterator() throws IOException {
        return new DocIdSetIterator() {

            int docID = -1;
            int docOffset = 0;
            int docIdSetIndex = 0;
            DocIdSetIterator currentDocIdSetIterator = (docIdSets.length == 0) ? null : docIdSets[0].iterator();

            @Override
            public int docID() {
                return docID;
            }

            @Override
            public int nextDoc() throws IOException {
                while (currentDocIdSetIterator != null) {
                    int currentDocIdSetDocId = currentDocIdSetIterator.nextDoc();
                    if (currentDocIdSetDocId != NO_MORE_DOCS) {
                        docID = docOffset + currentDocIdSetDocId;
                        return docID;
                    }
                    pointCurrentToNextIterator();
                }
                docID = NO_MORE_DOCS;
                return docID;
            }

            /**
             * if there is no next iterator, currentDocIdSetIterator becomes null
             */
            private void pointCurrentToNextIterator() throws IOException {
                currentDocIdSetIterator = null;
                while (currentDocIdSetIterator == null && docIdSetIndex + 1 < docIdSets.length) {
                    docOffset += maxDocs[docIdSetIndex];
                    docIdSetIndex++;
                    currentDocIdSetIterator = docIdSets[docIdSetIndex].iterator();
                }
            }

            @Override
            public int advance(final int target) throws IOException {
                while (currentDocIdSetIterator != null) {
                    int relative = target - docOffset;
                    if (relative < 0) {
                        relative = 0;
                    }
                    int currentDocIdSetDocId = currentDocIdSetIterator.advance(relative);
                    if (currentDocIdSetDocId != NO_MORE_DOCS) {
                        docID = docOffset + currentDocIdSetDocId;
                        return docID;
                    }
                    pointCurrentToNextIterator();
                }
                docID = NO_MORE_DOCS;
                return docID;
            }
        };
    }
}