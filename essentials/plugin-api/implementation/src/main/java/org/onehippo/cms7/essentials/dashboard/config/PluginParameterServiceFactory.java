/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.config;

import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * The PluginParameterServiceFactory attempts to derive a plugin parameter service from a plugin, or provides a default
 * service otherwise.
 */
public class PluginParameterServiceFactory {

    private static Logger log = LoggerFactory.getLogger(PluginParameterServiceFactory.class);

    public static PluginParameterService getParameterService(PluginRestful plugin) {
        final String parameterServiceClassName = plugin.getParameterServiceClass();

        if (StringUtils.hasText(parameterServiceClassName)) {
            try {
                Object object = Class.forName(parameterServiceClassName).newInstance();
                if (object instanceof PluginParameterService) {
                    return (PluginParameterService)object;
                } else {
                    log.warn("Parameter service class " + parameterServiceClassName + " implements unsupported class.");
                }
            } catch (Exception ex) {
                log.warn("Problem retrieving parameter service class " + parameterServiceClassName
                        + " for plugin " + plugin.getName() + ".", ex);
            }
        }

        return new DefaultPluginParameterService();
    }
}
