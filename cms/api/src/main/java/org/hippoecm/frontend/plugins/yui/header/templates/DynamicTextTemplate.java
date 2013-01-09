/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.yui.header.templates;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.JsFunction;
import org.hippoecm.frontend.plugins.yui.JsFunctionProcessor;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;

import java.io.Serializable;
import java.util.Map;

public class DynamicTextTemplate implements IHeaderContributor, IDetachable {

    private static final long serialVersionUID = 1L;

    private TextTemplateHeaderContributor headerContributor;
    private Map<String, Object> variables;

    private Serializable configuration;
    private String id;
    private String moduleClass;

    public DynamicTextTemplate(Class<?> clazz, String filename) {
        this(new PackagedTextTemplate(clazz, filename));
    }
    
    public DynamicTextTemplate(PackagedTextTemplate template) {
        headerContributor = TextTemplateHeaderContributor.forJavaScript(template, new AbstractReadOnlyModel<Map<String, Object>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Map<String, Object> getObject() {
                return DynamicTextTemplate.this.getVariables();
            }
        });
    }
    
    public DynamicTextTemplate(PackagedTextTemplate template, Serializable settings) {
        this(template);
        this.configuration = settings;
    }

    public void renderHead(IHeaderResponse response) {
        headerContributor.renderHead(response);
    }

    public void detach() {
        headerContributor.detach(null);
    }

    protected Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new MiniMap(5);
        }
        if (getSettings() != null) {
            variables.put("config", getConfigurationAsJSON());
        }
        if (getId() != null) {
            variables.put("id", getId());
        }
        if (getModuleClass() != null) {
            variables.put("class", getModuleClass());
        }
        return variables;
    }

    public final String getConfigurationAsJSON() {
        if (getSettings() instanceof YuiObject) {
            return ((YuiObject) getSettings()).toScript();
        } else if (getSettings() != null) {
            return JSONObject.fromObject(getSettings(), getJsonConfig()).toString();
        } else {
            return "null";
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setModuleClass(String moduleClass) {
        this.moduleClass = moduleClass;
    }

    public String getModuleClass() {
        return moduleClass;
    }

    public void setConfiguration(Serializable configuration) {
        this.configuration = configuration;
    }

    public Serializable getSettings() {
        return configuration;
    }

    public JsonConfig getJsonConfig() {
        JsonConfig config = new JsonConfig();
        Serializable settings = getSettings();
        if(settings != null && settings instanceof IAjaxSettings) {
            config.registerJsonValueProcessor(JsFunction.class, new JsFunctionProcessor());
        }
        return config;
    }

}
