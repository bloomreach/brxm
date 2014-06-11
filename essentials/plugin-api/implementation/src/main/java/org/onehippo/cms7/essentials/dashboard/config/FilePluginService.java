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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class FilePluginService extends AbstractPluginService {

    private static Logger log = LoggerFactory.getLogger(FilePluginService.class);

    public FilePluginService(final PluginContext context) {
        super(context);
    }

    @Override
    public boolean write(final Document document) {
        final String path = getFilePath(document, null);
        log.info("Writing settings to '{}'.", path);
        final File file = new File(path);
        try {
            //  make sure parent directory exists:
            final File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                FileUtils.forceMkdir(parentFile);
            }

            return marshalWriter(new FileWriter(file), document);
        } catch (IOException e) {
            log.error("Error writing to file '{}'.", path, e);
        }
        return false;
    }

    @Override
    public boolean write(final Document document, final String pluginId) {
        final String cleanedId = GlobalUtils.validFileName(pluginId);
        if (!Strings.isNullOrEmpty(cleanedId)) {
            document.setName(cleanedId);
        }
        return write(document);
    }

    @Override
    public <T extends Document> T read(final String pluginId, final Class<T> clazz) {
        final String cleanedId = (pluginId != null) ? GlobalUtils.validFileName(pluginId) : null;
        final String path = getFilePath(GlobalUtils.newInstance(clazz), cleanedId);
        if(!new File(path).exists()){
            log.debug("File '{}' not found.", path);
            return null;
        }
        log.debug("Reading settings from '{}'.", path);
        try {
            return unmarshalStream(new FileInputStream(path), clazz);
        } catch (IOException e) {
            log.error("Error reading file '{}'.", path, e);
        }
        return null;
    }

    @Override
    public <T extends Document> T read(final Class<T> clazz) {
        return read(null, clazz);
    }
}
