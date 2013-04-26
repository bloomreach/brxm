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

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class AutoCalcDeviceService extends DefaultDeviceService {

    private static Logger log = LoggerFactory.getLogger(AutoCalcDeviceService.class);

    /**
     * Construct a new Plugin.
     *
     * @param context the plugin context
     * @param config  the plugin config
     */
    public AutoCalcDeviceService(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    public StyleableDevice createStyleable(final IPluginContext context, final IPluginConfig config) {
        final int type = config.getInt("type", 1);
        switch (type) {
            case 2:
                return new StyleableTemplateDeviceModel(config);
            case 3:
                return new StyleableAutoCalculatingDeviceModel(config);
        }
        return new SimpleStylableDeviceModel(config);
    }


}
