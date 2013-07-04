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

import java.io.Serializable;
import java.util.Map;

import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.apache.wicket.util.template.TextTemplate;
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.JsFunction;
import org.hippoecm.frontend.plugins.yui.JsFunctionProcessor;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

public class DynamicTextTemplate implements IClusterable {

    private static final long serialVersionUID = 1L;

    private PackageTextTemplate template;
    private Map<String, Object> variables;

    private Serializable configuration;
    private String id;
    private String moduleClass;

    public DynamicTextTemplate(Class<?> clazz, String filename) {
        this(new PackageTextTemplate(clazz, filename));
    }

    public DynamicTextTemplate(PackageTextTemplate template) {
        this(template, null);
    }

    public DynamicTextTemplate(PackageTextTemplate template, Serializable settings) {
        this.template = template;
        this.configuration = settings;
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

    public CharSequence getString() {
        final TextTemplate textTemplate = template.interpolate(getVariables());
        return textTemplate.getString();
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
