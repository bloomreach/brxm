package org.hippoecm.frontend.plugins.yui.upload;

import net.sf.json.JsonConfig;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.JsFunction;
import org.hippoecm.frontend.plugins.yui.JsFunctionProcessor;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.layout.YuiId;
import org.hippoecm.frontend.plugins.yui.layout.YuiIdProcessor;

import java.io.Serializable;

public class AjaxMultiFileUploadBehavior extends AbstractYuiAjaxBehavior {
    final static String SVN_ID = "$Id$";

    private final PackagedTextTemplate behaviorJs = new PackagedTextTemplate(AjaxMultiFileUploadBehavior.class,
            "add_upload.js");


    DynamicTextTemplate template;

    public AjaxMultiFileUploadBehavior(final AjaxMultiFileUploadSettings settings) {
        super(settings);

        template = new DynamicTextTemplate(behaviorJs) {

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            public Serializable getSettings() {
                return settings;
            }

            @Override
            public JsonConfig getJsonConfig() {
                JsonConfig jsonConfig = new JsonConfig();
                jsonConfig.registerJsonValueProcessor(JsFunction.class, new JsFunctionProcessor());
                return jsonConfig;
            }
        };
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "upload");
        context.addTemplate(template);
        context.addOnWinLoad("YAHOO.hippo.Upload.render()");
    }

    @Override
    protected void respond(AjaxRequestTarget ajaxRequestTarget) {
    }
}
