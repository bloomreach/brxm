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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.PluginConfigEvent;

public class JavaPluginConfig extends ValueMap implements IPluginConfig {

    private static final long serialVersionUID = 1L;

    private Set<IPluginConfig> configSet = new LinkedHashSet<IPluginConfig>();

    private final int hashCode = new Object().hashCode();
    private String pluginInstanceName = null;
    private IObservationContext<IPluginConfig> obContext;

    public JavaPluginConfig() {
        super();
    }

    public JavaPluginConfig(String name) {
        super();
        pluginInstanceName = name;
    }

    public JavaPluginConfig(IPluginConfig parentConfig) {
        super();
        if (parentConfig != null) {
            putAll(parentConfig);
            for (IPluginConfig config : parentConfig.getPluginConfigSet()) {
                configSet.add(newPluginConfig(config));
            }
            pluginInstanceName = parentConfig.getName();
        }
    }

    protected IPluginConfig newPluginConfig(IPluginConfig source) {
        return new JavaPluginConfig(source);
    }
    
    public String getName() {
        return pluginInstanceName;
    }

    public Set<IPluginConfig> getPluginConfigSet() {
        return configSet;
    }

    @SuppressWarnings("unchecked")
    public IPluginConfig getPluginConfig(Object key) {
        Object value = get(key);
        if (value instanceof List && ((List) value).size() > 0) {
            return (IPluginConfig) ((List) value).get(0);
        }
        return (IPluginConfig) value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(String key, Object value) {
        if (value instanceof IPluginConfig) {
            value = new JavaPluginConfig((IPluginConfig) value);
        } else if (value instanceof List) {
            List<IPluginConfig> list = new ArrayList<IPluginConfig>(((List<IPluginConfig>) value).size());
            for (IPluginConfig entry : (List<IPluginConfig>) value) {
                list.add(new JavaPluginConfig(entry));
            }
            value = list;
        }
        Object oldValue = super.put((String) key, value);
        if (obContext != null) {
            EventCollection<IEvent<IPluginConfig>> collection = new EventCollection<IEvent<IPluginConfig>>();
            collection.add(new PluginConfigEvent(this, PluginConfigEvent.EventType.CONFIG_CHANGED));
            obContext.notifyObservers(collection);
        }
        return oldValue;
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        final Set<Map.Entry<String, Object>> entries = super.entrySet();
        return new AbstractSet<Map.Entry<String, Object>>() {

            @Override
            public Iterator<Map.Entry<String, Object>> iterator() {
                final Iterator<Map.Entry<String, Object>> orig = entries.iterator();
                return new Iterator<Map.Entry<String, Object>>() {

                    public boolean hasNext() {
                        return orig.hasNext();
                    }

                    public Map.Entry<String, Object> next() {
                        final Map.Entry<String, Object> entry = orig.next();
                        return new Map.Entry<String, Object>() {

                            public String getKey() {
                                return entry.getKey();
                            }

                            public Object getValue() {
                                return JavaPluginConfig.this.get(entry.getKey());
                            }

                            public Object setValue(Object value) {
                                return JavaPluginConfig.this.put(entry.getKey(), value);
                            }

                        };
                    }

                    public void remove() {
                        orig.remove();
                    }

                };
            }

            @Override
            public int size() {
                return entries.size();
            }

        };
    }

    // override super equals(), hashCode() as they depend on entry equivalence

    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @SuppressWarnings("unchecked")
    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        this.obContext = (IObservationContext<IPluginConfig>) context;
    }

    protected IObservationContext<? extends IPluginConfig> getObservationContext() {
        return obContext;
    }

    public void startObservation() {
    }

    public void stopObservation() {
    }

}
