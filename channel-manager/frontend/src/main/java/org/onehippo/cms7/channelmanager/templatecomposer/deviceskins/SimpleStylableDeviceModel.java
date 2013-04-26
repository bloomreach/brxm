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

import java.io.Serializable;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class SimpleStylableDeviceModel implements StyleableDevice, Serializable {

    private static Logger log = LoggerFactory.getLogger(SimpleStylableDeviceModel.class);

    protected final IPluginConfig config;
    private String name;

    public SimpleStylableDeviceModel(final IPluginConfig config) {
        this.config = config;
    }

    public String getStyle() {
        return config.containsKey("style") ? config.getString("style") : null;
    }

    @Override
    public String getWrapStyle() {
        return config.containsKey("wrapstyle") ? config.getString("wrapstyle") : null;
    }

    public String getName() {
        final String configName = config.getName();
        if (name == null) {
            name = configName.substring(configName.lastIndexOf('.') + 1);
        }
        return name;
    }

    @Override
    public String getId() {
        final String configName = config.getName();
        return configName.substring(configName.lastIndexOf('.') + 1);
    }

    public void setName(final String name) {
        this.name = name;
    }


}
