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
import java.util.TreeMap;
import java.util.TreeSet;

import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class ClusterConfigDecorator extends AbstractClusterDecorator {
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
        public Object get(final Object key) {
            Object value = super.get(key);
            if (value != null) {
                return value;
            } else if (ClusterConfigDecorator.this.values.containsKey(key)) {
                return ClusterConfigDecorator.this.values.get(key);
            } else if (ClusterConfigDecorator.this.getClusterKeys().contains(key)) {
                return ClusterConfigDecorator.this.get(key);
            }
            return null;
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            return new AbstractSet<Entry<String, Object>>() {

                final Set<Entry<String, Object>> upstreamSet = PluginConfigDecorator.super.entrySet();
                
                @Override
                public Iterator<Entry<String, Object>> iterator() {
                    return new Iterator<Entry<String, Object>>() {

                        final Set<String> done = new TreeSet<String>();
                        final Iterator<Entry<String, Object>> upstreamIter = upstreamSet.iterator();
                        Iterator<Entry<String, Object>> clusterKeyIter;

                        private Iterator<Entry<String, Object>> createClusterKeyIter() {
                            return new Iterator<Entry<String, Object>>() {

                                private final Iterator<String> clusterKeyIter;
                                {
                                    Set<String> clusterKeys = ClusterConfigDecorator.this.getClusterKeys();
                                    for (String key : done) {
                                        clusterKeys.remove(key);
                                    }
                                    clusterKeyIter = clusterKeys.iterator();
                                }

                                @Override
                                public boolean hasNext() {
                                    return clusterKeyIter.hasNext();
                                }

                                @Override
                                public Entry<String, Object> next() {
                                    return new Entry<String, Object>() {
                                        private final String key = clusterKeyIter.next();

                                        @Override
                                        public String getKey() {
                                            return key;
                                        }

                                        @Override
                                        public Object getValue() {
                                            return ClusterConfigDecorator.this.get(key);
                                        }

                                        @Override
                                        public Object setValue(final Object value) {
                                            throw new UnsupportedOperationException();
                                        }
                                    };
                                }

                                @Override
                                public void remove() {
                                    throw new UnsupportedOperationException();
                                }
                            };
                        }
                        
                        @Override
                        public boolean hasNext() {
                            if (upstreamIter.hasNext()) {
                                return true;
                            }
                            if (clusterKeyIter == null) {
                                clusterKeyIter = createClusterKeyIter();
                            }
                            return clusterKeyIter.hasNext();
                        }

                        @Override
                        public Entry<String, Object> next() {
                            if (upstreamIter.hasNext()) {
                                Entry<String, Object> entry = upstreamIter.next();
                                done.add(entry.getKey());
                                return entry;
                            }
                            if (clusterKeyIter == null) {
                                clusterKeyIter = createClusterKeyIter();
                            }
                            return clusterKeyIter.next();
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }

                @Override
                public int size() {
                    Set<String> keys = ClusterConfigDecorator.this.getClusterKeys();
                    int count = 0;
                    Iterator<Entry<String, Object>> iter = PluginConfigDecorator.super.entrySet().iterator();
                    while (iter.hasNext()) {
                        Entry<String, Object> entry = iter.next();
                        if (!keys.contains(entry.getKey())) {
                            count++;
                        }
                    }
                    return count + keys.size();
                }
            };
        }

        @Override
        protected Object decorate(Object object) {
            return ClusterConfigDecorator.this.decorate(object);
        }
    }

    private String clusterId;
    private Map<String, Object> values;

    public ClusterConfigDecorator(IClusterConfig upstream, final String clusterId) {
        super(upstream);
        this.clusterId = clusterId;
        this.values = new TreeMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(String strKey, Object value) {
        List<String>[] lists = new List[] { 
                ((IClusterConfig) upstream).getProperties(),
                ((IClusterConfig) upstream).getServices(),
                ((IClusterConfig) upstream).getReferences()
        };
        for (List<String> list : lists) {
            if (list.contains(strKey)) {
               return values.put(strKey, value); 
            }
        }
        return super.put(strKey, value);
    }
    
    @Override
    public Object get(Object key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }
        return super.get(key);
    }

    @Override
    protected Object decorate(Object object) {
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
        }
        return object;
    }

    private Set<String> getClusterKeys() {
        TreeSet<String> properties = new TreeSet<String>(values.keySet());
        for (Entry<String, Object> entry : super.entrySet()) {
            if (!(entry.getValue() instanceof IPluginConfig)) {
                properties.add(entry.getKey());
            }
        }
        return properties;
    }
}
