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
package org.hippoecm.frontend.plugins.yui.ajax;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBehaviorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AjaxIndicatorPlugin extends AjaxIndicatorBehavior implements IPlugin, IBehaviorService, IDetachable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AjaxIndicatorPlugin.class);

    private IPluginConfig config;

    public AjaxIndicatorPlugin(IPluginContext context, IPluginConfig config) {
        this.config = config;

        context.registerService(this, config.getString(ID));
    }

    public String getComponentPath() {
        return config.getString(IBehaviorService.PATH);
    }

    public void detach() {
        config.detach();
    }
}
