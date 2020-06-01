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
 * ValuelistNameProvider that uses a general (configured) base path plus the incoming message to construct the value
 * list node path.
 */
public class BasePathNameProvider implements IValueListNameProvider {
    static final Logger log = LoggerFactory.getLogger(BasePathNameProvider.class);

    /**
     * Concatenates a document name to a base path.
     *
     * @param documentName the name of the ValueList document node
     * @param config the plugin config that contains the base path property
     * @return concatenated path to the document or null if not found
     */
    @Override
    public String getValueListName(final String documentName, final IPluginConfig config) {
        final String baseSourcePath = config.getString(Config.SOURCE_BASE_PATH);
        if(StringUtils.isNotBlank(baseSourcePath)) {
            return SelectionUtils.ensureSlashes(baseSourcePath, documentName);
        } else {
            log.warn("Cannot find configuration property \"{}\" at config {}", Config.SOURCE_BASE_PATH, config);
        }
        return null;
    }

}
