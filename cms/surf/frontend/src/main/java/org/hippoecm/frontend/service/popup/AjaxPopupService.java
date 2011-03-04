/*
 *  Copyright 2011 Hippo.
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

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;

/**
 * Adds Javascript to the Wicket AJAX request target that opens a URL in a popup window. If the current Wicket
 * request target is not an AJAX request, nothing happens.
 */
public class AjaxPopupService extends Plugin implements IPopupService {

    public static final String SERVICE_ID = "service.popup.ajax";

    public AjaxPopupService(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final String serviceId = config.getString("service.id", IPopupService.DEFAULT_SERVICE_ID);
        context.registerService(this, serviceId);
    }

    @Override
    public void openPopupWindow(final String url) {
        if (url == null) {
            return;
        }

        IRequestTarget target = RequestCycle.get().getRequestTarget();

        if(target instanceof AjaxRequestTarget) {
            AjaxRequestTarget ajax = (AjaxRequestTarget) target;

            PopupSettings popupSettings = new PopupSettings(
                    PopupSettings.RESIZABLE
                    | PopupSettings.SCROLLBARS
                    | PopupSettings.LOCATION_BAR
                    | PopupSettings.MENU_BAR
                    | PopupSettings.TOOL_BAR);

            popupSettings.setTarget("'" + url + "'");

            StringBuffer javascript = new StringBuffer();
            javascript.append("(function() {");
            javascript.append(getPopupBlockerDetectionScript());
            javascript.append(popupSettings.getPopupJavaScript());
            javascript.append("})();");

            ajax.appendJavascript(javascript.toString());
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
