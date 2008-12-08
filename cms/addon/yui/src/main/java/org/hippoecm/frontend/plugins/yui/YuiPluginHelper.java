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
package org.hippoecm.frontend.plugins.yui;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.Settings;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public class YuiPluginHelper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";


    private static final String SERVICE_ID = "service.behavior.yui";
    private static final String CONFIG_ID = "yui.config";

    public static IYuiManager getManager(IPluginContext context) {
        return context.getService(SERVICE_ID, IYuiManager.class);
    }

    public static IPluginConfig getConfig(IPluginConfig config) {
        return config.getPluginConfig(CONFIG_ID);
    }
}
