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

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.frontend.model.map.AbstractValueMap;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class ClusterConfigDecorator extends JavaClusterConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private class PluginConfigDecorator extends AbstractValueMap implements IPluginConfig {
        private static final long serialVersionUID = 1L;

        private IPluginConfig conf;

        PluginConfigDecorator(IPluginConfig conf) {
            this.conf = conf;
        }

        public IPluginConfig getPluginConfig(Object key) {
            return (IPluginConfig) filter(conf.getPluginConfig(key));
        }

        public Set<IPluginConfig> getPluginConfigSet() {
            Set<IPluginConfig> result = new LinkedHashSet<IPluginConfig>();
            for (IPluginConfig config : conf.getPluginConfigSet()) {
                result.add((IPluginConfig) filter(config));
            }
            return result;
        }

        public void detach() {
            ClusterConfigDecorator.this.detach();
            conf.detach();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof PluginConfigDecorator) {
                PluginConfigDecorator that = (PluginConfigDecorator) other;
                return this.conf.equals(that.conf);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 31 * conf.hashCode();
        }

        @Override
        public Set entrySet() {
            final Set orig = conf.entrySet();
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
                                        Object obj = entry.getValue();
                                        Object result;
                                        if (obj.getClass().isArray()) {
                                            int size = Array.getLength(obj);
                                            Class<?> componentType = obj.getClass().getComponentType();
                                            result = Array.newInstance(componentType, size);
                                            for (int i = 0; i < size; i++) {
                                                Array.set(result, i, filter(Array.get(obj, i)));
                                            }
                                        } else {
                                            result = filter(obj);
                                        }
                                        return result;
                                    }

                                    public Object setValue(Object value) {
                                        return conf.put(entry.getKey(), value);
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

    }

    private IClusterConfig upstream;
    private List<String> overrides;
    private String clusterId;

    public ClusterConfigDecorator(IClusterConfig upstream, final String clusterId) {
        this.upstream = upstream;
        this.overrides = upstream.getOverrides();
        this.clusterId = clusterId;

        List<IPluginConfig> configs = upstream.getPlugins();
        for (IPluginConfig conf : configs) {
            addPlugin(new PluginConfigDecorator(conf));
        }
    }

    @Override
    public Object get(Object key) {
        Object obj = super.get(key);
        if (obj == null) {
            obj = upstream.get(key);
        }

        if (obj != null) {
            // Intercept values of the form "${" + variable + "}" + ...
            // These values are rewritten using the variables
            return filter(obj);
        }
        return null;
    }

    @Override
    public Object put(Object key, Object value) {
        Object old;
        if (overrides.contains(key)) {
            old = super.put(key, value);
        } else {
            old = upstream.put(key, value);
        }
        return old;
    }

    @Override
    public List<String> getOverrides() {
        return overrides;
    }

    @Override
    public void detach() {
        upstream.detach();
        super.detach();
    }

    private Object filter(Object object) {
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
            return new PluginConfigDecorator((IPluginConfig) object);
        } else if (object instanceof List) {
            final List list = (List) object;
            return new AbstractList() {

                @Override
                public Object get(int index) {
                    return filter(list.get(index));
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
