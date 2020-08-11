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
package org.hippoecm.hst.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A NOOP map implementation which can be used to extend for simple expression language maps.
 */
public class NOOPELMap implements Map<Object, Object>{
   
    public void clear() {}
    public boolean containsKey(Object key) {return false;}
    public boolean containsValue(Object value) {return false;}
    public Set<Entry<Object,Object>> entrySet() {return null;}
    public boolean isEmpty() {return false;}
    public Set<Object> keySet() {return null;}
    public Object put(Object key, Object value) {return null;}
    public void putAll(Map t) {}
    public Object remove(Object key) {return null;}
    public int size() {return 0;}
    public Collection<Object> values() {return null;}
    public Object get(Object key) {return null;}
}
