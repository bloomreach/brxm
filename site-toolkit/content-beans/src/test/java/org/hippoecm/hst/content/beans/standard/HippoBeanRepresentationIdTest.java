/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HippoBeanRepresentationIdTest {

    @Test
    public void returnsFalseWhenBothPathsAreNull() {
        TestHippoDocument documentBean = new TestHippoDocument("document-variant-uuid", "document-handle-uuid");
        TestHippoCompound compoundBean = new TestHippoCompound("compound-uuid-1");
        // Suppose you have a bean for a compound node, but it extends HippoDocument for some reason,
        // then it should return the identifier even if its underlying compound node cannot provide the handle id.
        TestHippoCompoundExtendingHippoDocument compoundBeanExtendingHippoDocument = new TestHippoCompoundExtendingHippoDocument(
                "compound-uuid-2");

        assertEquals(documentBean.getCanonicalHandleUUID(), documentBean.getRepresentationId());
        assertEquals(compoundBean.getIdentifier(), compoundBean.getRepresentationId());
        assertEquals(compoundBeanExtendingHippoDocument.getIdentifier(),
                compoundBeanExtendingHippoDocument.getRepresentationId());
    }

    class TestHippoDocument extends HippoDocument {

        private final String identifier;
        private final String canonicalHandleUUID;

        TestHippoDocument(final String identifier, final String canonicalHandleUUID) {
            this.identifier = identifier;
            this.canonicalHandleUUID = canonicalHandleUUID;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public String getCanonicalHandleUUID() {
            return canonicalHandleUUID;
        }
    }

    class TestHippoCompound extends HippoCompound {

        private final String identifier;

        TestHippoCompound(final String identifier) {
            this.identifier = identifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }
    }

    class TestHippoCompoundExtendingHippoDocument extends HippoDocument {

        private final String identifier;

        TestHippoCompoundExtendingHippoDocument(final String identifier) {
            this.identifier = identifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public String getCanonicalHandleUUID() {
            return null;
        }
    }

}
