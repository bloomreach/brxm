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

/**
 * ObjectIdentityKey is an object wrapper to be used as an object identity based key in a Collection Map or Set.
 * <p>
 *     Java itself <em>only</em> provides the {@link java.util.IdentityHashMap} class. When for example a
 *     {@link java.util.concurrent.ConcurrentHashMap} needs to be used,instead and the key object equals implementation
 *     isn't using object identity comparison, then this class can be used to wrap the key objects to enforce this.
 * </p>
 */
public class ObjectIdentityKey {
    private final Object object;

    public ObjectIdentityKey(final Object object) {
        this.object = object;
    }

    public int hashCode() {
        return object.hashCode();
    }

    public Object getObject() {
        return object;
    }

    public boolean equals(final Object other) {
        return other instanceof ObjectIdentityKey &&
                object == ((ObjectIdentityKey)other).object;
    }
}
