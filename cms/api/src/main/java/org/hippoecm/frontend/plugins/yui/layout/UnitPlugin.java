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
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBehaviorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special purpose {@link Plugin} that allows us to add the behavior through configuration instead of adding it 
 * in code to our component.  
 * 
 * See {@link IBehaviorService} and {@link IPlugin} for more info.
 */
public class UnitPlugin extends Plugin implements IBehaviorService {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(UnitPlugin.class);

    public UnitPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        context.registerService(this, config.getString(ID));
    }

    public String getComponentPath() {
        return getPluginConfig().getString(IBehaviorService.PATH);
    }

    public IBehavior getBehavior() {
        return new UnitBehavior(getPluginConfig().getString("position"));
    }
}
