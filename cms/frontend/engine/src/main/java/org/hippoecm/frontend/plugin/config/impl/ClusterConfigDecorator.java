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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class ClusterConfigDecorator extends JavaClusterConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IClusterConfig upstream;
    private List<String> overrides;
    private String clusterId;

    public ClusterConfigDecorator(IClusterConfig upstream, final String clusterId) {
        this.upstream = upstream;
        this.overrides = upstream.getOverrides();
        this.clusterId = clusterId;

        List<IPluginConfig> configs = upstream.getPlugins();
        for (final IPluginConfig conf : configs) {
            addPlugin(new JavaPluginConfig() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object get(Object key) {
                    Object obj = conf.get(key);
                    if (obj != null) {
                        if (obj instanceof String) {
                            return filter((String) obj);
                        } else if (obj.getClass().isArray()) {
                            Object[] list = (Object[]) obj;
                            Object[] result = new Object[list.length];
                            int i = 0;
                            for (Object item : list) {
                                if (item != null && item instanceof String) {
                                    result[i++] = filter((String) item);
                                } else {
                                    result[i++] = item;
                                }
                            }
                            return result;
                        }
                    }
                    return obj;
                }

                @Override
                public Set<Map.Entry> entrySet() {
                    return new AbstractSet<Map.Entry>() {

                        @Override
                        public Iterator<Map.Entry> iterator() {
                            final Iterator<Map.Entry> upstream = conf.entrySet().iterator();
                            return new Iterator<Map.Entry>() {

                                public boolean hasNext() {
                                    return upstream.hasNext();
                                }

                                public Map.Entry next() {
                                    final Map.Entry original = upstream.next();
                                    return new Map.Entry() {

                                        public Object getKey() {
                                            return original.getKey();
                                        }

                                        public Object getValue() {
                                            return get(original.getKey());
                                        }

                                        public Object setValue(Object value) {
                                            return put(original.getKey(), value);
                                        }
                                        
                                    };
                                }

                                public void remove() {
                                    upstream.remove();
                                }
                                
                            };
                        }

                        @Override
                        public int size() {
                            return conf.entrySet().size();
                        }
                        
                    };
                }
                
                @Override
                public Object put(Object key, Object value) {
                    return conf.put(key, value);
                }

                @Override
                public void detach() {
                    ClusterConfigDecorator.this.detach();
                    conf.detach();
                    super.detach();
                }
            });
        }
    }

    @Override
    public Object get(Object key) {
        Object obj = super.get(key);
        if (obj != null) {
            return obj;
        }

        obj = upstream.get(key);
        if ((obj != null) && (obj instanceof String)) {
            // Intercept values of the form "${" + variable + "}" + ...
            // These values are rewritten using the variables
            return filter((String) obj);
        }
        return obj;
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

    private Object filter(String value) {
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
    }

}
