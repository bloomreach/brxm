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
package org.hippoecm.frontend.plugins.yui.dragdrop;

import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.YuiHeaderContributor;
import org.hippoecm.frontend.service.render.RenderService;

public abstract class AbstractDragDropBehavior extends AbstractDefaultAjaxBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    protected IPluginConfig config;
    protected IPluginContext context;

    public AbstractDragDropBehavior(IPluginContext context, IPluginConfig config) {
        this.config = config;
        this.context = context;
    }

    @Override
    protected void onBind() {
        super.onBind();
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        YuiHeaderContributor.forModule(HippoNamespace.NS, "dragdropmanager").renderHead(response);
        TextTemplateHeaderContributor.forJavaScript(getHeaderContributorClass(), getHeaderContributorFilename(),
                new HeaderContributerModel()).renderHead(response);
        response.renderOnLoadJavascript("YAHOO.hippo.DragDropManager.onLoad()");
        super.renderHead(response);
    }

    @Override
    protected CharSequence getCallbackScript(boolean onlyTargetActivePage) {
        StringBuffer buf = new StringBuffer();
        buf.append("function doCallBack").append(getComponent().getMarkupId(true)).append("(myCallbackUrl){ ");
        buf.append(generateCallbackScript("wicketAjaxGet(myCallbackUrl")).append(" }");
        return buf.toString();
    }

    protected abstract String getHeaderContributorFilename();

    protected abstract Class<? extends IBehavior> getHeaderContributorClass();

    protected String getLabel() {
        String pluginModelId = config.getString(RenderService.MODEL_ID);
        if (pluginModelId != null) {
            IModelService pluginModelService = context.getService(pluginModelId, IModelService.class);
            if (pluginModelService != null) {
                IModel draggedModel = pluginModelService.getModel();
                if (draggedModel instanceof JcrNodeModel) {
                    JcrNodeModel nodeModel = (JcrNodeModel) draggedModel;
                    try {
                        return nodeModel.getNode().getDisplayName();
                    } catch (RepositoryException e) {
                        return getComponent().getMarkupId();
                    }
                }
            }
        }
        return getComponent().getMarkupId();
    }

    private class HeaderContributerModel extends AbstractReadOnlyModel {
        private static final long serialVersionUID = 1L;
        private Map<String, Object> variables;

        @Override
        public Object getObject() {
            if (variables == null) {
                variables = getHeaderContributorVariables();
            }
            return variables;
        }
    }
    
   private Map<String, Object> getHeaderContributorVariables() {
       Component component = getComponent();

       Map<String, Object> variables = new MiniMap(3);
       variables.put("id", component.getMarkupId(true));
       variables.put("class", getModelClass());
       variables.put("config", getJavacriptConfig());
       return variables;
   }
   
   protected JavascriptObjectMap getJavacriptConfig() {
       JavascriptObjectMap jsConfig = new JavascriptObjectMap();
       jsConfig.put("label", getLabel());
       jsConfig.put("groups", config.getStringArray("yui.dd.groups"));
       jsConfig.put("callbackUrl", getCallbackUrl().toString());
       jsConfig.put("callbackFunction", getCallbackScript().toString(), false);
       jsConfig.put("callbackParameters", getCallbackParameters());
       return jsConfig;
   }
   
   /**
    * Provide custom callbackParameters
    * @return JavascriptObjectMap containing key/value pairs that should be used as callbackParameters
    */
   protected JavascriptObjectMap getCallbackParameters() {
       return null;
   }
    
    /**
     * Specify the clientside class that is used as the DragDropModel 
     */
    abstract protected String getModelClass();
    
    static class JavascriptObjectMap {
        private static final String SINGLE_QUOTE = "'";
        private static final String SINGLE_QUOTE_ESCAPED = "\\'";
        private MiniMap map = new MiniMap(15);
        
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
            buf.append("]");
            store(key, buf.toString());
        }
        
        public void put(String key, JavascriptObjectMap values) {
            store(key, values);
        }
        
        private String escapeAndWrap(String value) {
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
