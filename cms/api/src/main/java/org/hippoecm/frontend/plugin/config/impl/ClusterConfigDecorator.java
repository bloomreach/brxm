/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

    private class PluginConfigDecorator extends AbstractPluginDecorator {

        PluginConfigDecorator(final IPluginConfig conf) {
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
            }
            value = ClusterConfigDecorator.this.decoratedValues.get(key);
            if (value != null) {
                return value;
            }
            value = ClusterConfigDecorator.this.get(key);
            if (value != null && !(value instanceof IPluginConfig)) {
                return value;
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

                        final Set<String> done = new TreeSet<>();
                        final Iterator<Entry<String, Object>> upstreamIter = upstreamSet.iterator();
                        Iterator<Entry<String, Object>> clusterKeyIter;

                        private Iterator<Entry<String, Object>> createClusterKeyIter() {
                            return new Iterator<Entry<String, Object>>() {

                                private final Iterator<String> clusterKeyIter;
                                {
                                    final Set<String> clusterKeys = getClusterKeys();
                                    for (final String key : done) {
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
                                final Entry<String, Object> entry = upstreamIter.next();
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
                    final Set<String> keys = getClusterKeys();
                    int count = 0;
                    for (final Entry<String, Object> entry : PluginConfigDecorator.super.entrySet()) {
                        if (!keys.contains(entry.getKey())) {
                            count++;
                        }
                    }
                    return count + keys.size();
                }
            };
        }

        private Set<String> getClusterKeys() {
            final TreeSet<String> properties = new TreeSet<>(decoratedValues.keySet());
            for (final Entry<String, Object> entry : ClusterConfigDecorator.super.entrySet()) {
                if (!(entry.getValue() instanceof IPluginConfig)) {
                    properties.add(entry.getKey());
                }
            }
            return properties;
        }

        @Override
        protected Object decorate(final Object object) {
            return ClusterConfigDecorator.this.decorate(object);
        }
    }

    private final String clusterId;
    private final transient Map<String, Object> decoratedValues;

    public ClusterConfigDecorator(final IClusterConfig upstream, final String clusterId) {
        super(upstream);
        this.clusterId = clusterId;
        this.decoratedValues = new TreeMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(final String strKey, final Object value) {
        final List<String>[] lists = new List[] {
                ((IClusterConfig) upstream).getProperties(),
                ((IClusterConfig) upstream).getServices(),
                ((IClusterConfig) upstream).getReferences()
        };
        for (final List<String> list : lists) {
            if (list.contains(strKey)) {
               return decoratedValues.put(strKey, value);
            }
        }
        return super.put(strKey, value);
    }

    @Override
    public Object get(final Object key) {
        if (decoratedValues.containsKey(key)) {
            return decoratedValues.get(key);
        }
        return super.get(key);
    }

    @Override
    protected Object decorate(final Object object) {
        if (object instanceof String) {
            final String value = (String) object;
            if (value.length() > 2 && value.charAt(0) == '$' && value.charAt(1) == '{') {
                final String variable = value.substring(2, value.lastIndexOf('}'));
                final String remainder = value.substring(value.lastIndexOf('}') + 1);
                if ("cluster.id".equals(variable)) {
                    return clusterId + remainder;
                } else {
                    final Object result = ClusterConfigDecorator.this.get(variable);
                    if (result instanceof String) {
                        return result + remainder;
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
}
