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

package org.hippoecm.frontend.plugins.yui.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.IClusterable;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.frontend.plugins.yui.YuiHeaderContributor;
import org.onehippo.yui.YuiNamespace;

public class HeaderContributorHelper implements IHeaderContributor{
    private static final long serialVersionUID = 1L;

    List<IHeaderContributor> modules = new LinkedList<IHeaderContributor>();
    List<IHeaderContributor> templates = new LinkedList<IHeaderContributor>();
    List<String> onloads = new LinkedList<String>();
    
    public void renderHead(IHeaderResponse response) {
        for (IHeaderContributor contrib: modules) {
            contrib.renderHead(response);
        }
        for (IHeaderContributor contrib: templates) {
            contrib.renderHead(response);
        }
        for(String onload : onloads) {
            response.renderOnLoadJavascript(onload);
        }
    }

    public void addModule(String module) {
        modules.add(YuiHeaderContributor.forModule(module));
    }

    public void addModule(YuiNamespace ns, String module) {
        modules.add(YuiHeaderContributor.forModule(ns, module));
    }

    public void addTemplate(Class<?> clazz, String filename, Map<String, Object> parameters ) {
        templates.add(new StaticTemplate(clazz, filename, parameters));
    }
    
    public void addTemplate(DynamicTemplate template) {
        templates.add(template);
    }

    public void addOnload(String string) {
        if(!onloads.contains(string))
            onloads.add(string);
    }
    
    class StaticTemplate implements IHeaderContributor {
        private static final long serialVersionUID = 1L;
        
        private TextTemplateHeaderContributor headerContributor;
        
        public StaticTemplate(PackagedTextTemplate template, Map<String, Object> parameters ) {
            headerContributor = TextTemplateHeaderContributor.forJavaScript(template, new StaticReadOnlyModel(parameters));
        }
        
        public StaticTemplate(Class<?> clazz, String filename, Map<String, Object> parameters ) {
            this(new PackagedTextTemplate(clazz, filename), parameters);
        }

        public void renderHead(IHeaderResponse response) {
            headerContributor.renderHead(response);
        }
    }
    
    private class  StaticReadOnlyModel extends AbstractReadOnlyModel {
        private static final long serialVersionUID = 1L;
        private Map<String, Object> values  = new HashMap<String, Object>();
        
        public StaticReadOnlyModel(Map<String, Object> values) {
            this.values.putAll(values);
        }

        @Override
        public Object getObject() {
            return values;
        }
    }

    abstract public class DynamicTemplate implements IHeaderContributor {
        private static final long serialVersionUID = 1L;
        
        private TextTemplateHeaderContributor headerContributor;
        private Map<String, Object> variables;
        
        public DynamicTemplate(Class<?> clazz, String filename) {
            this(new PackagedTextTemplate(clazz, filename));
        }
        
        public DynamicTemplate(PackagedTextTemplate template) {
            headerContributor = TextTemplateHeaderContributor.forJavaScript(template, new DynamicReadOnlyModel() {
                private static final long serialVersionUID = 1L;
                
                @Override
                Map<String, Object> getVariables() {
                    return DynamicTemplate.this.getVariables();
                }
            });
        }
        
        Map<String, Object> getVariables() {
            if(variables  == null) {
                variables = new MiniMap(5);
            }
            variables.put("config", getJsConfig());
            return variables;
        }

        abstract public JsConfig getJsConfig();

        public void renderHead(IHeaderResponse response) {
            headerContributor.renderHead(response);
        }
    }
    
    abstract public class HippoTemplate extends DynamicTemplate {
        private static final long serialVersionUID = 1L;
        
        private String moduleClass;
        
        public HippoTemplate(PackagedTextTemplate template, String moduleClass) {
            super(template);
            this.moduleClass = moduleClass;
        }
        
        public HippoTemplate(Class<?> clazz, String filename, String moduleClass) {
            this(new PackagedTextTemplate(clazz, filename), moduleClass);
        }
        
        @Override
        Map<String, Object> getVariables() {
            Map<String, Object> vars = super.getVariables();
            vars.put("id", getId());
            vars.put("class", moduleClass);
            return vars;
        }
        
        abstract public String getId();
    }
    
    private abstract class  DynamicReadOnlyModel extends AbstractReadOnlyModel {
        private static final long serialVersionUID = 1L;

        @Override
        public Object getObject() {
            return getVariables();
        }
        
        abstract Map<String, Object> getVariables();
    }

    static public class JsConfig implements IClusterable {
        private static final long serialVersionUID = 1L;
        
        private static final String SINGLE_QUOTE = "'";
        private static final String SINGLE_QUOTE_ESCAPED = "\\'";
        private MiniMap map = new MiniMap(5);
        
        private void store(String key, Object value) {
            ensureCapacity();
            map.put(key, value);
        }
        
        private void ensureCapacity() {
            if(map.isFull()) {
                MiniMap newMap = new MiniMap(map.size()*2);
                newMap.putAll(map);
                map = newMap;
            }
        }

        /**
         * Store boolean value
         */
        public void put(String key, boolean value) {
           store(key, Boolean.toString(value));
        }

        /**
         * Store int value
         */
        public void put(String key, int value) {
            store(key, Integer.toString(value));
        }
        
        /**
         * Store double value
         * @param key
         * @param value
         */
        public void put(String key, double value) {
            store(key, Double.toString(value));
        }
        
        /**
         * Convenience method, auto wraps and escapes String value
         * @param key
         * @param value
         */
        public void put(String key, String value) {
            put(key, value, true);
        }

        /**
         * 
         * @param key
         * @param value
         * @param escapeAndWrap
         */
        public void put(String key, String value, boolean escapeAndWrap) {
            //escape single quotes and wrap
            if(escapeAndWrap) {
                value = escapeAndWrap(value);
            }
            store(key, value);
        }

        public void put(String key, String[] values) {
            put(key, values, true);
        }
        
        public void put(String key, String[] values, boolean escapeAndWrap) {
            StringBuilder buf = new StringBuilder();
            buf.append('[');
            if(values != null) {
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        buf.append(',');
                    }
                    if(escapeAndWrap) {
                        buf.append(escapeAndWrap(values[i]));
                    } else {
                        buf.append(values[i]);    
                    }
                }
            }
            buf.append(']');
            store(key, buf.toString());
        }
        
        public void put(String key, Map<String, String> map) {
            put(key, map, true);
        }
        
        public void put(String key, Map<String, String> schemaMetaFields, boolean escapeAndWrap) {
            String value = null;
            if(schemaMetaFields != null) {
                StringBuilder buf = new StringBuilder();
                buf.append('{');
                boolean first = true;
                for(Entry<String, String> e : schemaMetaFields.entrySet()) {
                    if(first) { 
                        first = false;
                    } else {
                        buf.append(',');
                    }
                    buf.append(e.getKey()).append(':');
                    if(escapeAndWrap) {
                        buf.append(escapeAndWrap(e.getValue()));
                    } else {
                        buf.append(e.getValue());
                    }
                }
                buf.append('}');
                value = buf.toString();
            }
            store(key, value);
        }
        
        public void put(String key, JsConfig values) {
            store(key, values);
        }
        
        private String escapeAndWrap(String value) {
            //TODO: backslash should be escaped as well
            value = SINGLE_QUOTE + value.replace(SINGLE_QUOTE, SINGLE_QUOTE_ESCAPED) + SINGLE_QUOTE;
            return value;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first= true;

            for(Object o : map.entrySet()) {
                Entry e = (Entry)o;
                if(first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(e.getKey()).append(':').append(e.getValue());
            }
            sb.append('}');
            return sb.toString();
        }

    }

}
