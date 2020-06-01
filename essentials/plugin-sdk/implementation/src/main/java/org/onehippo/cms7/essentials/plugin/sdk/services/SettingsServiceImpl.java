/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.File;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.plugin.sdk.config.PluginFileService;
import org.onehippo.cms7.essentials.plugin.sdk.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.plugin.sdk.utils.ProjectUtils;
import org.onehippo.cms7.essentials.sdk.api.model.ProjectSettings;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.springframework.stereotype.Service;

@Service
public class SettingsServiceImpl implements SettingsService {

    private static final String DEFAULT_NAME = "project-settings";

    private final PluginFileService pluginFileService;

    private ProjectSettingsBean settings; // in-memory cached copy of settings
    private File projectSettingsFile;
    private long lastModified;            // last modified timestamp for reloading settings upon FS change

    @Inject
    public SettingsServiceImpl(final PluginFileService pluginFileService) {
        this.pluginFileService = pluginFileService;
    }

    @Override
    public ProjectSettings getSettings() {
        return getModifiableSettings();
    }

    public ProjectSettingsBean getModifiableSettings() {
        if (projectSettingsFile == null) {
            projectSettingsFile = ProjectUtils.getEssentialsResourcesRootPath().resolve("project-settings.xml").toFile();
        }

        final long lastModified = projectSettingsFile.lastModified();
        if (settings == null || lastModified != this.lastModified) {
            this.lastModified = lastModified;
            settings = pluginFileService.read(DEFAULT_NAME, ProjectSettingsBean.class);
        }

        return settings;
    }

    /**
     * Set settings and persist to filesystem.
     */
    public boolean updateSettings(final ProjectSettingsBean settings) {
        this.settings = settings;
        return pluginFileService.write(DEFAULT_NAME, settings);
    }
}
