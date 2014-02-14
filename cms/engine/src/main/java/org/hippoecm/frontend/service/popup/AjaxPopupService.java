/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service.popup;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.service.IPopupService;

/**
 * Adds Javascript to the Wicket AJAX request target that opens a URL in a popup window. If the current Wicket request
 * target is not an AJAX request, nothing happens.
 */
public class AjaxPopupService extends Plugin implements IPopupService {

    public static final String SERVICE_ID = "service.popup.ajax";

    public AjaxPopupService(IPluginContext context, IPluginConfig config) {
        super(context, config);
        final String name = config.getString(SERVICE_ID, IPopupService.class.getName());
        context.registerService(this, name);
    }

    @Override
    public void openPopupWindow(final PopupSettings popupSettings, final String url) {
        if (url == null) {
            return;
        }
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            popupSettings.setTarget("'" + url + "'");

            StringBuffer javascript = new StringBuffer();
            javascript.append("(function() {");
            javascript.append(getPopupBlockerDetectionScript());
            javascript.append(popupSettings.getPopupJavaScript());
            javascript.append("})();");

            target.appendJavaScript(javascript.toString());
        }
    }

    private String getPopupBlockerDetectionScript() {
        final ClassResourceModel messageModel = new ClassResourceModel("popup.blocker.detected", getClass());

        StringBuilder sb = new StringBuilder();
        sb.append("var popupDetectionTest = window.open('', '', 'width=1,height=1,left=0,top=0,scrollbars=no');");
        sb.append("if(!popupDetectionTest) {");
        sb.append("alert('").append(messageModel.getObject()).append("'); return false;");
        sb.append("} else { popupDetectionTest.close(); }");

        return sb.toString();
    }

}
