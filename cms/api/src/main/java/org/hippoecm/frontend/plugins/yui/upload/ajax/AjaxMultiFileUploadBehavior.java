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

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;

public class AjaxMultiFileUploadBehavior extends AbstractYuiAjaxBehavior {

    DynamicTextTemplate template;

    public AjaxMultiFileUploadBehavior(final AjaxMultiFileUploadSettings settings) {
        super(settings);

        template = new DynamicTextTemplate(new PackageTextTemplate(getClass(), "add_upload.tpl")) {

            @Override
            public String getId() {
                return getComponent().getMarkupId();
            }

            @Override
            public Serializable getSettings() {
                final Model<AjaxMultiFileUploadSettings> settingsModel = Model.of(settings);
                final String suffix = settings.getMaxNumberOfFiles() > 1 ? ".files.caption" : ".file.caption";

                settings.addTranslation("select.caption", getComponent().getString("select" + suffix, settingsModel));
                settings.addTranslation("browse.caption", getComponent().getString("browse" + suffix, settingsModel));
                settings.addTranslation("list.caption", getComponent().getString("list" + suffix, settingsModel));

                return settings;
            }

        };
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addCssReference(new CssResourceReference(AjaxMultiFileUploadBehavior.class, "res/skin.css"));
        context.addModule(HippoNamespace.NS, "upload");
        context.addTemplate(template);
        context.addOnDomLoad("YAHOO.hippo.Upload.render()");
    }

    @Override
    protected void respond(AjaxRequestTarget ajaxRequestTarget) {
        HttpServletRequest r = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
        if (r.getParameter("finished") != null && r.getParameter("finished").equals("true")) {
            if (r.getParameter("scrollPosY") != null) {
                ajaxRequestTarget.appendJavaScript("YAHOO.hippo.Upload.restoreScrollPosition(" + r.getParameter(
                        "scrollPosY") + ");");
            }
            onFinish(ajaxRequestTarget);
        }
    }

    protected void onFinish(AjaxRequestTarget ajaxRequestTarget) {
    }

}
