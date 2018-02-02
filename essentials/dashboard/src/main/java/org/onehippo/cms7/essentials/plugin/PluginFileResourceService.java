/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin;

import java.io.InputStream;

import org.onehippo.cms7.essentials.plugin.sdk.config.PluginFileService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to read settings from Essentials WAR resources.
 */
public class PluginFileResourceService extends PluginFileService {

    private static Logger log = LoggerFactory.getLogger(PluginFileResourceService.class);

    PluginFileResourceService(final ProjectService projectService) {
        super(projectService);
    }

    @Override
    public boolean write(final String filename, final Object data) {
        throw new IllegalStateException("Writing a plugin file into a JAR is not supported.");
    }

    @Override
    public <T> T read(final String filename, final Class<T> clazz) {
        final String sanitized = sanitizeFileName(filename);
        if (sanitized == null) {
            return null;
        }

        final String path = "/" + sanitized;
        final InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            log.debug("Resource {} not found.", path);
            return null;
        }
        log.debug("Reading settings of: {}", path);
        return GlobalUtils.unmarshalStream(stream, clazz);
    }
}
