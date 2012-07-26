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
package org.hippoecm.frontend.model.map;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HippoMap extends HashMap<String, Object> implements IHippoMap {

    private static final long serialVersionUID = 1L;

    private String primaryType = "nt:unstructured";
    private List<String> mixins = new LinkedList<String>();

    public HippoMap() {
    }
    
    public HippoMap(IHippoMap original) {
        super(original);

        primaryType = original.getPrimaryType();
        mixins = new ArrayList<String>(original.getMixinTypes());
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(String type) {
        primaryType = type;
    }

    public List<String> getMixinTypes() {
        return mixins;
    }

    public void addMixinType(String type) {
        mixins.add(type);
    }

    public void removeMixinType(String type) {
        mixins.remove(type);
    }

    public void reset() {
        throw new UnsupportedOperationException("Cannot reset HippoMap");
    }

    public void save() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(String key, Object value) {
        if (value instanceof List) {
            List<IHippoMap> newList = new LinkedList<IHippoMap>();
            for (IHippoMap entry : (List<IHippoMap>) value) {
                newList.add(new HippoMap(entry));
            }
            value = newList;
        }
        return super.put(key, value);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        final Set<Map.Entry<String, Object>> base = super.entrySet();
        return new AbstractSet<Map.Entry<String, Object>>() {

            @Override
            public Iterator<Map.Entry<String, Object>> iterator() {
                final Iterator<Map.Entry<String, Object>> iter = base.iterator();
                return new Iterator<Map.Entry<String, Object>>() {

                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    public Map.Entry<String, Object> next() {
                        final Map.Entry<String, Object> entry = iter.next();
                        return new Map.Entry<String, Object>() {

                            public String getKey() {
                                return entry.getKey();
                            }

                            public Object getValue() {
                                return HippoMap.this.get(entry.getKey());
                            }

                            public Object setValue(Object value) {
                                return HippoMap.this.put(entry.getKey(), value);
                            }
                            
                        };
                    }

                    public void remove() {
                        iter.remove();
                    }
                    
                };
            }

            @Override
            public int size() {
                return base.size();
            }
            
        };
    }

}
