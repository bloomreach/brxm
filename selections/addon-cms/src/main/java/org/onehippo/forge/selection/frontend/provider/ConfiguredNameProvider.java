/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.frontend.provider;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.selection.frontend.plugin.Config;
import org.onehippo.forge.selection.frontend.utils.SelectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ValuelistNameProvider that looks up each name from the plugin configuration.
 */
public class ConfiguredNameProvider implements IValueListNameProvider {
    static final Logger log = LoggerFactory.getLogger(ConfiguredNameProvider.class);

    /**
     * Looks up the valuelist name by key from the plugin config.
     *
     * @param key used to find the configured valuelist name.
     * @param config the plugin config
     * @return path from the config or null if not found
     */
    @Override
    public String getValueListName(final String key, final IPluginConfig config) {
        final String sourcePath = config.getString(Config.SOURCE + "." + key);
        if (StringUtils.isNotBlank(sourcePath)) {
            return sourcePath;
        }

        log.warn("Cannot find configuration property \"" + Config.SOURCE + ".{}\" at config {}", key, config);
        return null;
    }
}
