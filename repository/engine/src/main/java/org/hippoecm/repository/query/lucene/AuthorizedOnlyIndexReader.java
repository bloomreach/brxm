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
import java.util.BitSet;

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermPositions;

public class AuthorizedOnlyIndexReader extends FilterIndexReader {

    /**
     * The deleted documents as initially read from the IndexReader passed
     * in the constructor of this class.
     */
    private final BitSet authorized;

    public AuthorizedOnlyIndexReader(IndexReader in, BitSet authorized) {
        super(in);
        this.authorized = authorized;
    }

    @Override
    public TermDocs termDocs(Term term) throws IOException {
        // do not wrap for empty TermDocs
        TermDocs td = in.termDocs(term);
        td = new AuthorizedTermDocs(td);
        return td;
    }

    @Override
    public TermDocs termDocs() throws IOException {
        return new AuthorizedTermDocs(super.termDocs());
    }

    @Override
    public TermPositions termPositions() throws IOException {
        return new AuthorizedTermPositions(super.termPositions());
    }

    private class AuthorizedTermDocs extends FilterTermDocs {
        public AuthorizedTermDocs(TermDocs in) {
            super(in);
        }

        @Override
        public boolean next() throws IOException {
            boolean hasNext = super.next();
            while (hasNext && !authorized.get(super.doc())) {
                hasNext = super.next();
            }
            return hasNext;
        }

        @Override
        public boolean skipTo(int i) throws IOException {
            boolean exists = super.skipTo(i);
            while (exists && !authorized.get(doc())) {
                exists = next();
            }
            return exists;
        }
    }

    //---------------------< FilteredTermPositions >----------------------------
    /**
     * Filters a wrapped TermPositions by omitting documents marked as deleted.
     */
    private final class AuthorizedTermPositions extends AuthorizedTermDocs
            implements TermPositions {

        public AuthorizedTermPositions(TermPositions in) {
            super(in);
        }

        public int nextPosition() throws IOException {
            return ((TermPositions) this.in).nextPosition();
        }

        public int getPayloadLength() {
            return ((TermPositions) in).getPayloadLength();
        }

        public byte[] getPayload(byte[] data, int offset) throws IOException {
            return ((TermPositions) in).getPayload(data, offset);
        }

        public boolean isPayloadAvailable() {
            return ((TermPositions) in).isPayloadAvailable();
        }
    }
}
