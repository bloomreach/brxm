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

import java.io.File;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Created by tjeger on 6/4/14.
 */
public abstract class AbstractPluginService implements PluginConfigService {

    private static final String SETTINGS_EXTENSION = ".xml";
    private static Logger log = LoggerFactory.getLogger(AbstractPluginService.class);
    private final PluginContext context;

    public AbstractPluginService(final PluginContext context) {
        this.context = context;
    }

    @Override
    public abstract boolean write(Document document);

    @Override
    public abstract boolean write(Document document, String pluginId);

    @Override
    public abstract <T extends Document> T read(final String pluginId, final Class<T> clazz);

    @Override
    public abstract <T extends Document> T read(final Class<T> clazz);

    @Override
    public void close() {
        // do nothing
    }

    /**
     * Determine name of a configuration file
     *
     * @param document   instance of a document represented by the configuration file
     * @param pluginName name of a plugin (may be null)
     * @return name of the corresponding configuraion file.
     */
    protected static String getFileName(final Document document, final String pluginName) {
        String fileName = pluginName;
        if (Strings.isNullOrEmpty(fileName)) {
            if (Strings.isNullOrEmpty(document.getName())) {
                fileName = GlobalUtils.validFileName(document.getClass().getName());
            } else {
                fileName = GlobalUtils.validFileName(document.getName());
            }
        }
        if (!fileName.endsWith(SETTINGS_EXTENSION)) {
            fileName = fileName + SETTINGS_EXTENSION;
        }
        return fileName;
    }

    protected String getFilePath(final Document document, final String pluginName) {
        return context.getEssentialsResourcePath() + File.separator + getFileName(document, pluginName);
    }

}