/**
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.resourcebundle;

import java.util.Enumeration;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.collections.iterators.IteratorEnumeration;

/**
 * MapResourceBundle
 * <P>
 * Useful utility class if a ResourceBundle object is needed from a Map.
 * </P>
 */
public class MapResourceBundle extends ResourceBundle {

    private Map<String, ? extends Object> map;

    public MapResourceBundle(Map<String, ? extends Object> map) {
        this.map = map;
    }

    /**
     * Gets a resource for a given key. This is called by <code>getObject</code>.
     *
     * @param key the key of the resource
     * @return the resource for the key, or null if it doesn't exist
     */
    @Override
    public final Object handleGetObject(String key) {
        return map.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return new IteratorEnumeration(map.keySet().iterator());
    }

}
