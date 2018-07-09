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
package org.onehippo.cms7.util;

import java.util.HashSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class ObjectIdentityKeyTest {

    /**
     * Trivial wrapper class to ensure the <em>opposite</em> behavior of the {@link ObjectIdentityKey}
     * for testing the contract. Note that Object equality is the default (natural) behavior but specific
     * 'key' classes can override this (like the {@link ObjectIdentityKey}), so this test class needs a
     * guaranteed behavior to validate its opposite behavior.
     */
    private static class ObjectEqualityKey {
        private final Object object;

        public ObjectEqualityKey(final Object object) {
            this.object = object;
        }

        public int hashCode() {
            return object.hashCode();
        }

        public boolean equals(final Object other) {
            return other instanceof ObjectEqualityKey &&
                    object.equals(((ObjectEqualityKey)other).object);
        }
    }

    @Test
    public void test() {
        HashSet<Object> set = new HashSet<>();
        Object one1 = new ObjectEqualityKey("one");
        Object one2 = new ObjectEqualityKey("one");
        assertEquals(one1, one2);
        assertNotSame(one1, one2);
        set.add(one1);
        set.add(one2);
        assertEquals(1, set.size());
        set.clear();
        one1 = new ObjectIdentityKey(one1);
        one2 = new ObjectIdentityKey(one2);
        assertNotEquals(one1, one2);
        set.add(one1);
        set.add(one2);
        assertEquals(2, set.size());
        assertTrue(set.contains(one1));
        assertTrue(set.contains(one2));
        assertFalse(set.contains(new ObjectIdentityKey(new ObjectEqualityKey("one"))));
    }
}
