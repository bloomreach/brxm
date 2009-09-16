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

package org.hippoecm.frontend.plugins.standards.behaviors;

import org.apache.wicket.behavior.IBehavior;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.behaviors.BrowserSpecificStylesheetsBehavior.Browser;
import org.hippoecm.frontend.plugins.standards.behaviors.BrowserSpecificStylesheetsBehavior.StylesheetConfiguration;
import org.hippoecm.frontend.plugins.standards.behaviors.BrowserSpecificStylesheetsBehavior.UserAgent;
import org.hippoecm.frontend.service.IBehaviorService;

public class BrowserSpecificStylesheetsPlugin implements IPlugin, IBehaviorService {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private String componentPath;
    private BrowserSpecificStylesheetsBehavior behavior;

    public BrowserSpecificStylesheetsPlugin(IPluginContext context, IPluginConfig config) {
        componentPath = config.getString(IBehaviorService.PATH);

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
        behavior = new BrowserSpecificStylesheetsBehavior(configurations);

        context.registerService(this, config.getString(ID));
    }

    public IBehavior getBehavior() {
        return behavior;
    }

    public String getComponentPath() {
        return componentPath;
    }

}
