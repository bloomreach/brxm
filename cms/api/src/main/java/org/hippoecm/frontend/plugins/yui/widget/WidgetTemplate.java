/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.widget;

import java.io.Serializable;
import java.util.Map;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.apache.wicket.util.template.TextTemplate;
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.JsFunction;
import org.hippoecm.frontend.plugins.yui.JsFunctionProcessor;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

public class WidgetTemplate implements IHeaderContributor, IDetachable {
    private static final long serialVersionUID = 1L;

    private final TextTemplate WIDGET_TEMPLATE = new PackageTextTemplate(WidgetTemplate.class, "widget_template.js");

    private Map<String, Object> variables;

    private String namespace;
    private String clazz;
    private String method;
    private String id;
    private Serializable configuration;
    private String instance;

    public WidgetTemplate() {
        namespace = "YAHOO.hippo";
        clazz = "WidgetManager";
        method = "register";
        instance = "YAHOO.hippo.Widget";
    }

    public void renderHead(IHeaderResponse response) {
        response.render(OnDomReadyHeaderItem.forScript(WIDGET_TEMPLATE.asString(getVariables())));
    }

    public void detach() {
    }

    protected Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new MiniMap<String, Object>(6);
        }

        variables.put("namespace", getNamespace());
        variables.put("class", getClazz());
        variables.put("method", getMethod());
        variables.put("id", getId());
        Serializable serializable = getConfiguration();
        JsonConfig jsonConfig = internalGetJsonConfig(serializable);
        variables.put("config", getAsJSON(serializable, jsonConfig));
        variables.put("instance", getInstance());
        return variables;
    }

    public static String getAsJSON(Serializable serializable, JsonConfig jsonConfig) {
        if (serializable != null) {
            return JSONObject.fromObject(serializable, jsonConfig).toString();
        } else {
            return "null";
        }
    }

    private JsonConfig internalGetJsonConfig(Serializable serializable) {
        JsonConfig jsonConfig = new JsonConfig();
        decorateJsonConfig(jsonConfig);
        if (serializable != null && serializable instanceof IAjaxSettings) {
            jsonConfig.registerJsonValueProcessor(JsFunction.class, new JsFunctionProcessor());
        }
        return jsonConfig;
    }

    protected void decorateJsonConfig(JsonConfig jsonConfig) {
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setConfiguration(Serializable configuration) {
        this.configuration = configuration;
    }

    public Serializable getConfiguration() {
        return configuration;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getInstance() {
        return instance;
    }

}
