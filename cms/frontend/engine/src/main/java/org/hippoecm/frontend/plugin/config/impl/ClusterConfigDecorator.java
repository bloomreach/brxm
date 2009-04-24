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

import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class ClusterConfigDecorator extends JavaClusterConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private class PluginConfigDecorator extends AbstractPluginDecorator {
        private static final long serialVersionUID = 1L;

        PluginConfigDecorator(IPluginConfig conf) {
            super(conf);
        }

        @Override
        public String getName() {
            return clusterId + ".plugin." + super.getName();
        }

        @Override
        protected Object decorate(Object object) {
            return ClusterConfigDecorator.this.decorate(object);
        }
    }

    private String clusterId;

    public ClusterConfigDecorator(IClusterConfig upstream, final String clusterId) {
        super(upstream);
        this.clusterId = clusterId;
    }

    @Override
    public Object get(Object key) {
        Object obj = super.get(key);
        if (obj != null) {
            // Intercept values of the form "${" + variable + "}" + ...
            // These values are rewritten using the variables
            return decorate(obj);
        }
        return null;
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
                                    return ClusterConfigDecorator.this.get(entry.getKey());
                                }

                                public Object setValue(Object value) {
                                    return ClusterConfigDecorator.this.put(entry.getKey(), value);
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

    @Override
    protected IPluginConfig newPluginConfig(IPluginConfig config) {
        return new PluginConfigDecorator(config);
    }

    private Object decorate(Object object) {
        if (object instanceof String) {
            String value = (String) object;
            if (value.length() > 2 && value.charAt(0) == '$' && value.charAt(1) == '{') {
                String variable = value.substring(2, value.lastIndexOf('}'));
                String remainder = value.substring(value.lastIndexOf('}') + 1);
                if ("cluster.id".equals(variable)) {
                    return clusterId + remainder;
                } else {
                    Object result = ClusterConfigDecorator.this.get(variable);
                    if (result instanceof String) {
                        return ((String) result) + remainder;
                    } else {
                        return result;
                    }
                }
                // unreachable
            }
            return value;
        } else if (object instanceof IPluginConfig) {
            return newPluginConfig((IPluginConfig) object);
        } else if (object instanceof List) {
            final List list = (List) object;
            return new AbstractList() {

                @Override
                public Object get(int index) {
                    return decorate(list.get(index));
                }

                @Override
                public int size() {
                    return list.size();
                }

            };
        }
        return object;
    }

}
