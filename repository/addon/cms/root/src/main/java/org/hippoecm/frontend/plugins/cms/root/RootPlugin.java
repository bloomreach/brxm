/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.root;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.behavior.HeaderContributor;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.ajax.AjaxIndicatorBehavior;
import org.hippoecm.frontend.plugins.yui.layout.YuiWireframeBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.widgets.Pinger;

public class RootPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public RootPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString(RenderService.SKIN_ID) != null) {
            add(HeaderContributor.forCss(config.getString(RenderService.SKIN_ID)));
        }

        YuiWireframeBehavior rootWireframe = new YuiWireframeBehavior("");
        Map<String, String> opts = new HashMap<String, String>();
        opts.put("height", "1");
        opts.put("id", "root-layout-header");
        rootWireframe.addUnit("top", opts);

        opts = new HashMap<String, String>();
        opts.put("id", "root-layout-center");
        opts.put("gutter", "0 0px 0 0px");
        rootWireframe.addUnit("center", opts);
        
        opts = new HashMap<String, String>();
        opts.put("height", "0");
        opts.put("id", "root-layout-footer");
        rootWireframe.addUnit("bottom", opts);
        
        add(rootWireframe);

        add(new AjaxIndicatorBehavior());
        add(new Pinger("pinger"));
    }

}
