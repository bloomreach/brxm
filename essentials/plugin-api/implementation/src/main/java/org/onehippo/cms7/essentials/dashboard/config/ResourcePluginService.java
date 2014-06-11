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

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Service to read settings from WAR resources (write operations are implemented as no-op.
 */
public class ResourcePluginService extends AbstractPluginService {

    private static Logger log = LoggerFactory.getLogger(ResourcePluginService.class);

    public ResourcePluginService(final PluginContext context) {
        super(context);
    }

    @Override
    public boolean write(Document document) {
        log.warn("Writing to WAR resource not supported");
        return false;
    }

    @Override
    public boolean write(Document document, String pluginId) {
        return write(document);
    }

    @Override
    public <T extends Document> T read(final String pluginId, final Class<T> clazz) {
        final String cleanedId = (pluginId != null) ? GlobalUtils.validFileName(pluginId) : null;
        final String path = File.separator + getFileName(GlobalUtils.newInstance(clazz), cleanedId);
        final InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            log.debug("Resource {} not found.", path);
            return null;
        }
        log.debug("Reading settings of: {}", path);
        return unmarshalStream(stream, clazz);
    }

    @Override
    public <T extends Document> T read(final Class<T> clazz) {
        return read(null, clazz);
    }
}
