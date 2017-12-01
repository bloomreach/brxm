/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.project;

import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;

/**
 * Utility functions for an Essentials project.
 */
public class ProjectUtils {

    private static boolean initialized;

    private ProjectUtils() {
    }

    public static void persistSettings(final PluginContext context, final ProjectSettings settings) throws Exception {
        try (PluginConfigService configService = context.getConfigService()) {
            configService.write(settings);
        }
    }

    public static ProjectSettings loadSettings(final PluginContext context) {
        // TODO: more symmetry!
        return context.getProjectSettings();
    }

    public static void setInitialized(final boolean init) {
        initialized = init;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
