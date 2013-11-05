/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.internal;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * A String pool utility that can be used to return an already present String object from the heap instead
 * of creating new Strings for String objects that are equal. 
 * Since the hst configuration object contains many equal strings for multiple sites, this saves lots of memory. 
 */
public class StringPool {

    private static final WeakHashMap<String, WeakReference<String>> stringPool = new WeakHashMap<>(1000);

    /**
     * not allowed to instantiate
     */
    private StringPool(){}

    /**
     * If <code>string</code> is already present in the pool (same hashcode and equals), then the already present String 
     * object is returned. Else, the <code>string</code> is added to the stringPool. 
     * @param string The {@link String} object for which to get its cached object instance
     * @return the String object from the argument or if their was already and equal object in the pool, the object that was already there.
     */
    public static synchronized String get(String string) {
        if(string == null) {
            return null;
        }
        WeakReference<String> weakVal = stringPool.get(string);
        String pooledString;
        if (weakVal != null && (pooledString = weakVal.get()) != null) {
            return pooledString;
        }
        stringPool.put(string, new WeakReference<>(string));
        return string;
    }

    public static synchronized int size() {
        return stringPool.size();
    }

    /**
     * Clears the entire StringPool
     */
    public static synchronized void clear() {
        stringPool.clear();
    }
}
