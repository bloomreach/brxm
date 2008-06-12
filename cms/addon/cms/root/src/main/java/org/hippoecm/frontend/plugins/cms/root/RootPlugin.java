/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.root;

import org.apache.wicket.behavior.HeaderContributor;
import org.hippoecm.frontend.dialog.DialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.ajax.AjaxIndicatorBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;

public class RootPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    public RootPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        for (String extension : new String[] { "logoutPlugin", "tabsPlugin" }) {
            addExtensionPoint(extension);
        }
        DialogService dialogService = new DialogService();
        dialogService.init(context, config.getString(RenderService.DIALOG_ID), "dialog");
        add(dialogService);
        
        if (config.getString(RenderService.SKIN_ID) != null) {
            add(HeaderContributor.forCss(config.getString(RenderService.SKIN_ID)));
        }
        
        add(new AjaxIndicatorBehavior());
    }

}
