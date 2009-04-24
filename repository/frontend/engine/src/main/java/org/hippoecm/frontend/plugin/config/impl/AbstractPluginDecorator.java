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
package org.hippoecm.frontend.plugin.config.impl;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public abstract class AbstractPluginDecorator extends JavaPluginConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public AbstractPluginDecorator(IPluginConfig upstream) {
        super(upstream);
    }

    @Override
    public Object get(Object key) {
        Object obj = super.get(key);
        if (obj == null) {
            return obj;
        }
        Object result;
        if (obj.getClass().isArray()) {
            int size = Array.getLength(obj);
            Class<?> componentType = obj.getClass().getComponentType();
            result = Array.newInstance(componentType, size);
            for (int i = 0; i < size; i++) {
                Array.set(result, i, decorate(Array.get(obj, i)));
            }
        } else {
            result = decorate(obj);
        }
        return result;
    }

    @Override
    public Set entrySet() {
        final Set orig = super.entrySet();
        return new AbstractSet() {

            @Override
            public Iterator iterator() {
                final Iterator origIter = orig.iterator();
                return new Iterator() {

                    public boolean hasNext() {
                        return origIter.hasNext();
                    }

                    public Object next() {
                        final Entry entry = (Map.Entry) origIter.next();
                        if (entry != null) {
                            return new Map.Entry() {

                                public Object getKey() {
                                    return entry.getKey();
                                }

                                public Object getValue() {
                                    return AbstractPluginDecorator.this.get(entry.getKey());
                                }

                                public Object setValue(Object value) {
                                    return AbstractPluginDecorator.this.put(entry.getKey(), value);
                                }

                            };
                        }
                        return null;
                    }

                    public void remove() {
                        origIter.remove();
                    }

                };
            }

            @Override
            public int size() {
                return orig.size();
            }

        };
    }

    protected abstract Object decorate(Object object);
}
