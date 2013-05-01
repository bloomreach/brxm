/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DefaultDeviceService extends Plugin implements DeviceService  {

    private static Logger log = LoggerFactory.getLogger(DefaultDeviceService.class);

    protected final List<StyleableDevice> styleables = new ArrayList<StyleableDevice>();

    /**
     * Construct a new Plugin.
     *
     * @param context the plugin context
     * @param config  the plugin config
     */
    public DefaultDeviceService(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config != null && config.getString("deviceskins.service.id") != null) {
            context.registerService(this, config.getString("deviceskins.service.id"));
        }

        styleables.add(new StyleableDeviceImpl("default"));
        styleables.add(new StyleableDeviceImpl("iphone_landscape"));
        styleables.add(new StyleableDeviceImpl("iphone_portrait"));
        styleables.add(new StyleableDeviceImpl("ipad_landscape"));
        styleables.add(new StyleableDeviceImpl("ipad_portrait"));

    }

    @Override
    public List<StyleableDevice> getStylables() {
        return Collections.unmodifiableList(styleables);
    }

}
