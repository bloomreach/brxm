/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials;

import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;

/**
 * Mocks a plugin context which uses fake project settings rather than trying to read them from the filesystem.
 */
public class MockPluginContext extends DefaultPluginContext {
    public static final String TEST_NAMESPACE = "testnamespace";
    public static final String TEST_PROJECT_PACKAGE = "org.onehippo.cms7.essentials.dashboard.test";

    private static final ProjectSettingsBean projectSettings;

    static {
        projectSettings = new ProjectSettingsBean();

        projectSettings.setProjectNamespace(TEST_NAMESPACE);
        projectSettings.setSelectedProjectPackage(TEST_PROJECT_PACKAGE);
        projectSettings.setSelectedBeansPackage(TEST_PROJECT_PACKAGE + ".beans");
        projectSettings.setSelectedComponentsPackage(TEST_PROJECT_PACKAGE + ".components");
        projectSettings.setSelectedRestPackage(TEST_PROJECT_PACKAGE + ".rest");
    }

    @Override
    public ProjectSettings getProjectSettings() {
        return projectSettings;
    }
}
