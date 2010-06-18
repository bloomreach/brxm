package org.hippoecm.frontend.plugins.yui.widget;

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

import java.io.Serializable;
import java.util.Map;

public class WidgetTemplate implements IHeaderContributor, IDetachable {
    private static final long serialVersionUID = 1L;

    final static String SVN_ID = "$Id$";

    private TextTemplateHeaderContributor headerContributor;
    private Map<String, Object> variables;

    private String namespace;
    private String clazz;
    private String method;
    private String id;
    private Serializable configuration;

    public WidgetTemplate() {
        PackagedTextTemplate template = new PackagedTextTemplate(WidgetTemplate.class, "widget_template.js");
        headerContributor = TextTemplateHeaderContributor.forJavaScript(template,
                new AbstractReadOnlyModel<Map<String, Object>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Map<String, Object> getObject() {
                        return WidgetTemplate.this.getVariables();
                    }
                });

        namespace = "YAHOO.hippo";
        clazz = "WidgetManager";
        method = "register";
    }

    public void renderHead(IHeaderResponse response) {
        headerContributor.renderHead(response);
    }

    public void detach() {
        headerContributor.detach(null);
    }

    protected Map<String, Object> getVariables() {
        if (variables == null) {
            variables = new MiniMap<String, Object>(5);
        }

        variables.put("namespace", getNamespace());
        variables.put("class", getClazz());
        variables.put("method", getMethod());
        variables.put("id", getId());

        Serializable serializable = getConfiguration();
        JsonConfig jsonConfig = internalGetJsonConfig(serializable);
        variables.put("config", getAsJSON(serializable, jsonConfig));
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

}
