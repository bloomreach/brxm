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
package org.hippoecm.frontend.plugin.config.impl;

import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuiltinApplicationFactory implements IApplicationFactory {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BuiltinApplicationFactory.class);

    public BuiltinApplicationFactory() {
    }

    public IPluginConfigService getDefaultApplication() {
        return getApplication("console");
    }

    public IPluginConfigService getApplication(String name) {
        log.info("Starting builtin application: " + name);
        JavaConfigService configService;
        if ("login".equals(name)) {
            configService = new JavaConfigService("login");
        } else {
            configService = new JavaConfigService("console");
        }
        configService.addClusterConfig("login", initLogin());
        configService.addClusterConfig("console", initConsole());
        return configService;
    }

    private IClusterConfig initLogin() {
        JavaClusterConfig plugins = new JavaClusterConfig();

        IPluginConfig config = new JavaPluginConfig("login");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.login.LoginPlugin");
        config.put("wicket.id", "service.root");
        plugins.addPlugin(config);

        return plugins;
    }

    private IClusterConfig initConsole() {
        JavaClusterConfig plugins = new JavaClusterConfig();

        IPluginConfig config = new JavaPluginConfig("root");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.RootPlugin");
        config.put("wicket.id", "service.root");
        config.put("wicket.dialog", "service.dialog");
        config.put("wicket.model", "service.model");

        config.put("wicket.extensions", new String[] { "extension.top", "extension.left", "extension.center" });
        config.put("extension.top", "service.top");
        config.put("extension.left", "service.left");
        config.put("extension.center", "service.center");
//        config.put("wicket.behavior", new String[] { "service.behavior.yui", "service.layout.main" });
        JavaPluginConfig yuiConfig = new JavaPluginConfig("yui.config");
        yuiConfig.put("body.gutter", "0px 10px 5px 0px");
        yuiConfig.put("body.scroll", true);
        yuiConfig.put("header.gutter", "0px 10px 0px 10px");
        yuiConfig.put("header.height", "71");
        yuiConfig.put("left.gutter", "0px 0px 5px 10px");
        yuiConfig.put("left.width", "460");
        yuiConfig.put("left.resize", true);
        config.put("yui.config", yuiConfig);
        plugins.addPlugin(config);

        config = new JavaPluginConfig("top");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.TopPlugin");
        config.put("wicket.id", "service.top");

        config.put("wicket.extensions", new String[] { "extension.breadcrumb", "extension.logout", "extension.menu",
                "extension.check" });
        config.put("extension.breadcrumb", "service.breadcrumb");
        config.put("extension.menu", "service.menu");
        config.put("extension.check", "service.check");
        config.put("extension.logout", "service.logout");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("left");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.LeftPlugin");
        config.put("wicket.id", "service.left");
        config.put("wicket.behavior", new String[] { "service.layout.left" });

        config.put("wicket.extensions", new String[] { "extension.sorter", "extension.tree" });
        config.put("extension.tree", "service.tree");
        config.put("extension.sorter", "service.sorter");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("sorter");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.menu.SortMenuPlugin");
        config.put("wicket.id", "service.sorter");
        config.put("wicket.model", "service.model");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("tree");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.browser.BrowserPlugin");
        config.put("wicket.id", "service.tree");
        config.put("wicket.model", "service.model");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("center");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.editor.EditorPlugin");
        config.put("wicket.id", "service.center");
        config.put("wicket.model", "service.model");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("breadcrumb");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.breadcrumb.BreadcrumbPlugin");
        config.put("wicket.id", "service.breadcrumb");
        config.put("wicket.model", "service.model");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("menu");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.menu.MenuPlugin");
        config.put("wicket.id", "service.menu");
        config.put("wicket.model", "service.model");
        config.put("wicket.dialog", "service.dialog");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("check");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.console.menu.CheckPlugin");
        config.put("wicket.id", "service.check");
        plugins.addPlugin(config);

        config = new JavaPluginConfig("logout");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.logout.LogoutPlugin");
        config.put("wicket.id", "service.logout");
        config.put("wicket.model", "service.model");
        config.put("wicket.dialog", "service.logout.dialog");
        plugins.addPlugin(config);

/*
        config = new JavaPluginConfig("webappBehavior");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.yui.webapp.WebAppPlugin");
        config.put("behavior.id", "service.behavior.yui");
        JavaPluginConfig yuiConfig = new JavaPluginConfig("yui.config");
        yuiConfig.put("load.css.fonts", false);
        yuiConfig.put("load.css.grids", false);
        yuiConfig.put("load.css.reset", true);
        yuiConfig.put("load.wicket.ajax", true);
        config.put("yui.config", yuiConfig);
        plugins.addPlugin(config);

        config = new JavaPluginConfig("layoutBehavior");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.yui.layout.PageLayoutPlugin");
        config.put("behavior.id", "service.layout.main");
        yuiConfig = new JavaPluginConfig("yui.config");
        yuiConfig.put("body.gutter", "0px 10px 5px 0px");
        yuiConfig.put("body.scroll", true);
        yuiConfig.put("header.gutter", "0px 10px 0px 10px");
        yuiConfig.put("header.height", "71");
        yuiConfig.put("left.gutter", "0px 0px 5px 10px");
        yuiConfig.put("left.width", "460");
        yuiConfig.put("left.resize", true);
        yuiConfig.put("root.id", "doc3");
        config.put("yui.config", yuiConfig);
        plugins.addPlugin(config);
*/

        config = new JavaPluginConfig("browserBehavior");
        config.put("plugin.class", "org.hippoecm.frontend.plugins.yui.layout.WireframePlugin");
        config.put("behavior.id", "service.layout.left");
        yuiConfig = new JavaPluginConfig("yui.config");
        yuiConfig.put("root.id", "navigator-wrapper");
        yuiConfig.put("units", new String[] { "top", "center" });
        yuiConfig.put("top", "id=navigator-top,height=24");
        yuiConfig.put("center", "id=navigator-center,body=navigator-center-body,scroll=true");
        yuiConfig.put("linked.with.parent", true);
        config.put("yui.config", yuiConfig);
        plugins.addPlugin(config);

        return plugins;
    }

}
