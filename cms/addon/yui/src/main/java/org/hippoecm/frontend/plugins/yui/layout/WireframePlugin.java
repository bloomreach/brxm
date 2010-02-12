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
package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.behavior.IBehavior;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.service.IBehaviorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireframePlugin extends WireframeBehavior implements IPlugin, IBehaviorService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(WireframePlugin.class);

    public static final String UNITS = "yui.units";
    public static final String WRAPPERS = "yui.wrappers";
    public static final String ROOT = "yui.root";
    public static final String LINKED = "yui.linked";
    public static final String CLIENT_CLASSNAME = "yui.classname";

    private IPluginConfig config;

    public WireframePlugin(IPluginContext context, IPluginConfig config) {
        super(new WireframeSettings(YuiPluginHelper.getConfig(config)));

        this.config = config;
        context.registerService(this, config.getString(ID));
    }

    public IBehavior getBehavior() {
        return this;
    }

    public String getComponentPath() {
        return config.getString(IBehaviorService.PATH);
    }

    public void start() {
    }

    public void stop() {
    }

}
