/*
 *  Copyright 2010 Hippo.
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
