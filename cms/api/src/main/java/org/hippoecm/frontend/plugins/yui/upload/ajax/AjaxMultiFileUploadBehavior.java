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
package org.hippoecm.frontend.plugins.yui.upload.ajax;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

public class AjaxMultiFileUploadBehavior extends AbstractYuiAjaxBehavior {

    DynamicTextTemplate template;

    public AjaxMultiFileUploadBehavior(final AjaxMultiFileUploadSettings settings) {
        super(settings);

        template = new DynamicTextTemplate(new PackagedTextTemplate(getClass(), "add_upload.js")) {

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            public Serializable getSettings() {
                settings.addTranslation("select.files.link",
                   settings.isAllowMultipleFiles() ? new StringResourceModel("select.files.link", getComponent(), null).getString() :
                   settings.isUploadAfterSelect() ? new StringResourceModel("upload.file.link", getComponent(), null).getString() :
                                                    new StringResourceModel("select.file.link", getComponent(), null).getString()
                );
                return settings;
            }

        };
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addCssReference(new ResourceReference(AjaxMultiFileUploadBehavior.class, "res/skin.css"));
        context.addModule(HippoNamespace.NS, "upload");
        context.addTemplate(template);
        context.addOnWinLoad("YAHOO.hippo.Upload.render()");
    }

    @Override
    protected void respond(AjaxRequestTarget ajaxRequestTarget) {
        HttpServletRequest r = ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest();
        if (r.getParameter("finished") != null && r.getParameter("finished").equals("true")) {
            if (r.getParameter("scrollPosY") != null) {
                ajaxRequestTarget.appendJavascript("YAHOO.hippo.Upload.restoreScrollPosition(" + r.getParameter(
                        "scrollPosY") + ");");
            }
            onFinish(ajaxRequestTarget);
        }
    }

    protected void onFinish(AjaxRequestTarget ajaxRequestTarget) {
    }

}
