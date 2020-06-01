/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

/**
 * Helper class for retrieving an {@link IYuiManager} from the provided {@link IPluginContext} and 
 * an {@link IPluginConfig} instance with name "yui.config" from the provided {@link IPluginConfig} parent.  
 */
public class YuiPluginHelper {


    private static final String SERVICE_ID = "service.behavior.yui";
    private static final String CONFIG_ID = "yui.config";

    /**
     * Check if the provided {@link IPluginConfig} has a child {@link IPluginConfig} with 
     * name "yui.config". If so, return it, else return null.
     *  
     * @param config
     *            The {@link IPluginConfig} to will be used as lookup entrypoint.
     *                               
     * @return An {@link IPluginConfig} instance or null if not found
     */
    public static IPluginConfig getConfig(IPluginConfig config) {
        IPluginConfig subConfig = config.getPluginConfig(CONFIG_ID);
        if (subConfig == null) {
            return new JavaPluginConfig();
        }
        return subConfig;
    }
}
