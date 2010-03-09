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

import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.root.BrowserSpecificStylesheetsBehavior.Browser;
import org.hippoecm.frontend.plugins.cms.root.BrowserSpecificStylesheetsBehavior.StylesheetConfiguration;
import org.hippoecm.frontend.plugins.cms.root.BrowserSpecificStylesheetsBehavior.UserAgent;
import org.hippoecm.frontend.plugins.standards.tabs.TabsPlugin;
import org.hippoecm.frontend.plugins.yui.ajax.AjaxIndicatorBehavior;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutBehavior;
import org.hippoecm.frontend.plugins.yui.layout.PageLayoutSettings;
import org.hippoecm.frontend.plugins.yui.layout.UnitBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppBehavior;
import org.hippoecm.frontend.plugins.yui.webapp.WebAppSettings;
import org.hippoecm.frontend.widgets.Pinger;

public class RootPlugin extends TabsPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private boolean rendered = false;

    public RootPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new Pinger("pinger"));
        add(new LogoutLink("logout"));

        PageLayoutSettings plSettings = new PageLayoutSettings();
        plSettings.setHeaderHeight(50);
        // TODO: update settings from config
        add(new PageLayoutBehavior(plSettings));

        add(new AjaxIndicatorBehavior());

        String[] browsers = config.getStringArray("browsers");
        StylesheetConfiguration[] configurations = new StylesheetConfiguration[browsers.length];
        for (int i = 0; i < browsers.length; i++) {
            if (config.containsKey(browsers[i])) {
                IPluginConfig browserConf = config.getPluginConfig(browsers[i]);
                String ua = browserConf.getString("user.agent", "unsupported").toUpperCase();
                Browser browser = new Browser(UserAgent.valueOf(ua), browserConf.getInt("major.version", -1),
                        browserConf.getInt("minor.version", -1));

                configurations[i] = new StylesheetConfiguration(browser, browserConf.getStringArray("stylesheets"));
            }
        }
        add(new BrowserSpecificStylesheetsBehavior(configurations));

        get("tabs").add(new WireframeBehavior(new WireframeSettings(config.getPluginConfig("layout.wireframe"))));
        get("tabs:panel-container").add(new UnitBehavior("center"));
        get("tabs:tabs-container").add(new UnitBehavior("left"));
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (!rendered) {
            WebAppSettings settings = new WebAppSettings();
            settings.setLoadCssFonts(true);
            settings.setLoadCssGrids(true);
            settings.setLoadCssReset(true);
            getPage().add(new WebAppBehavior(settings));
            rendered = true;
        }
        super.render(target);
    }

}
