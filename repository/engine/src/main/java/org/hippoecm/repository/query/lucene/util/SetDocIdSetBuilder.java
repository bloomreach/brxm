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
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetDocIdSetBuilder {

    private static final Logger log = LoggerFactory.getLogger(SetDocIdSetBuilder.class);
    private final List<DocIdSet> docIdSets = new ArrayList<DocIdSet>();

    /**
     * @param docIdSet the docIdSet to add. <code>docIdSet</code> is allowed to be <code>null</code> in which case it is
     *                 ignored
     */
    public void add(DocIdSet docIdSet) {
        if (docIdSet != null) {
            docIdSets.add(docIdSet);
        }
    }

    public OpenBitSet toBitSet() throws IOException {
        long start = System.currentTimeMillis();
        final int size = docIdSets.size();
        DocIdSetIterator[] iterators = new DocIdSetIterator[size];
        for (int i = 0; i < size; i++) {
            iterators[i] = docIdSets.get(i).iterator();
            if (iterators[i] == null) {
                return new OpenBitSet();
            }
        }

        OpenBitSet bitSet = new OpenBitSet();
        if (size == 0) {
            return bitSet;
        }

        int currentDoc = -1;
        int currentIter = -1;
        int iterIndex = 0;
        while (currentDoc != DocIdSetIterator.NO_MORE_DOCS) {
            if (iterIndex == currentIter) {
                bitSet.set(currentDoc);
                currentDoc = -1;
            }

            int newDoc;
            if (currentDoc == -1) {
                newDoc = iterators[iterIndex].nextDoc();
            } else {
                newDoc = iterators[iterIndex].advance(currentDoc);
            }

            if (newDoc > currentDoc) {
                currentIter = iterIndex;
                currentDoc = newDoc;
            }
            if (++iterIndex == size) {
                iterIndex = 0;
            }
        }
        log.info("Creating OpenBitSet of lenght '{}' for '{}' DocIdSet's took '{}' ms.",
                new String[]{String.valueOf(bitSet.length()), String.valueOf(size), String.valueOf(System.currentTimeMillis() - start)});
        return bitSet;
    }

}
