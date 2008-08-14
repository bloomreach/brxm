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

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBehaviorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YuiWireframePlugin extends YuiWireframeBehavior implements IPlugin, IBehaviorService, IDetachable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(YuiWireframePlugin.class);

    public static final String UNITS = "yui.units";
    public static final String WRAPPERS = "yui.wrappers";
    public static final String ROOT = "yui.root";
    public static final String LINKED = "yui.linked";

    private IPluginConfig config;

    public YuiWireframePlugin(IPluginContext context, IPluginConfig config) {
        super(config.getString(ROOT), config.getBoolean(LINKED));

        String[] units = config.getStringArray(UNITS);
        if (units != null) {
            for (String unit : units) {
                String serialized = config.getString(unit);
                ValueMap map;
                if (serialized != null) {
                    map = new ValueMap(config.getString(unit));
                } else {
                    map = new ValueMap();
                    log.warn("No config found for unit {}", unit);
                }
                addUnit(unit, map);
            }
        }

        String[] wrappers = config.getStringArray(WRAPPERS);
        if (wrappers != null) {
            for (String position : wrappers) {
                registerUnitElement(position, config.getString(position));
            }
        }

        if (units == null && wrappers == null) {
            log.warn("No units defined");
        }

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
