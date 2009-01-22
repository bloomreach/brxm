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
package org.hippoecm.hst.caching;

import java.io.Serializable;

public class CacheKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String key;

    public CacheKey(String key, Class clazz) {
        this.key = clazz.getName() + "_" + key;
    }

    public String getKey() {
        return key;
    }

    public String toString() {
        return key;
    }

    public boolean equals(Object object) {
        if (object != null && object instanceof CacheKey) {
            CacheKey ck = (CacheKey) object;
            return ck.getKey().equals(this.key);
        }
        return false;
    }

    public int hashCode() {
        return key.hashCode();
    }

}
