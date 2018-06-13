/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import java.util.HashMap;

/**
 * A  HashMap<K,V> that throws an exception when you put in the same key twice
 *
 * @param <K>
 * @param <V>
 */
public class DuplicateKeyNotAllowedHashMap<K, V> extends HashMap<K, V> {
    
    private static final long serialVersionUID = 1L;

    @Override
    public V put(K key, V value) throws IllegalArgumentException {
        V prev = get(key);
        if (prev != null) {
            throw new DuplicateKeyException("DuplicateKeyNotAllowedHashMap is not allowed to have duplicate keys: The key '"+key+"' is already present");
        }
        return super.put(key, value);
    }

    public static class DuplicateKeyException extends IllegalArgumentException {
        public DuplicateKeyException(final String s) {
            super(s);
        }
    }
    
}

