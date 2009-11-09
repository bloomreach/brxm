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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    @Override
    public Object put(Object key, Object value) {
        String strKey = (String) key;
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

}
