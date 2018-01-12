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

package org.onehippo.cms7.essentials.plugin.sdk.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import com.google.common.base.Strings;

import org.apache.commons.io.FileUtils;
import org.onehippo.cms7.essentials.plugin.sdk.model.TargetPom;
import org.onehippo.cms7.essentials.plugin.sdk.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class PluginFileService {
    private static final Logger log = LoggerFactory.getLogger(PluginFileService.class);

    private final ProjectService projectService;

    public PluginFileService(final ProjectService projectService) {
        this.projectService = projectService;
    }

    public boolean write(final String filename, final Object data) {
        final File file = getFile(filename);
        if (file == null) {
            return false;
        }

        final File directory = file.getParentFile();
        try {
            if (!directory.exists()) {
                FileUtils.forceMkdir(directory);
            }

            log.debug("Writing settings to '{}'.", file);
            return GlobalUtils.marshalWriter(new FileWriter(file), data);
        } catch (IOException e) {
            log.error("Error writing to file '{}'.", file, e);
        }
        return false;
    }

    public <T> T read(final String filename, final Class<T> clazz) {
        final File file = getFile(filename);
        if (file == null) {
            return null;
        }

        if (!file.exists()) {
            log.debug("File '{}' not found.", file);
            return null;
        }
        log.debug("Reading settings from '{}'.", file);
        try {
            return GlobalUtils.unmarshalStream(new FileInputStream(file), clazz);
        } catch (IOException e) {
            log.error("Error reading file '{}'.", file, e);
        }
        return null;
    }

    private File getFile(final String filename) {
        final String sanitized = sanitizeFileName(filename);
        if (sanitized == null) {
            return null;
        }

        return projectService.getResourcesRootPathForModule(TargetPom.ESSENTIALS).resolve(sanitized).toFile();
    }

    protected String sanitizeFileName(final String candidate) {
        final String sanitized = GlobalUtils.validFileName(candidate);
        if (Strings.isNullOrEmpty(sanitized)) {
            log.error("Filename '{}' is not considered valid.", candidate);
            return null;
        }

        return sanitized + ".xml";
    }
}
