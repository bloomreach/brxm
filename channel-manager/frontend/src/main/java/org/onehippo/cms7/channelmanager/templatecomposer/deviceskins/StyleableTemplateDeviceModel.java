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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.util.string.interpolator.MapVariableInterpolator;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class StyleableTemplateDeviceModel extends SimpleStyleableDeviceModel {

    private static Logger log = LoggerFactory.getLogger(StyleableTemplateDeviceModel.class);

    protected final Map<String, Object> templatedProperties = new HashMap<String, Object>();

    public StyleableTemplateDeviceModel(final IPluginConfig config) {
        super(config);
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            templatedProperties.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String getStyle() {
        return process(super.getStyle());
    }

    @Override
    public String getWrapStyle() {
        return process(super.getWrapStyle());
    }

    protected final String process(final String style) {
        if (style == null) return null;
        return MapVariableInterpolator.interpolate(style, templatedProperties);
    }

}
