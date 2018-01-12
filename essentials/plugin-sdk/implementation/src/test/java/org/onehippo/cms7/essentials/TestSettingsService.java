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

package org.onehippo.cms7.essentials;

import org.onehippo.cms7.essentials.plugin.sdk.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.plugin.sdk.service.SettingsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("base-test")
@Configuration
public class TestSettingsService {

    public static final String PROJECT_NAMESPACE_TEST = "testnamespace";

    private static final String TEST_PROJECT_PACKAGE = "org.onehippo.cms7.essentials.dashboard.test";
    private final ProjectSettingsBean projectSettings;

    public TestSettingsService() {
        projectSettings = new ProjectSettingsBean();

        projectSettings.setProjectNamespace(PROJECT_NAMESPACE_TEST);
        projectSettings.setSelectedProjectPackage(TEST_PROJECT_PACKAGE);
        projectSettings.setSelectedBeansPackage(TEST_PROJECT_PACKAGE + ".beans");
        projectSettings.setSelectedComponentsPackage(TEST_PROJECT_PACKAGE + ".components");
        projectSettings.setSelectedRestPackage(TEST_PROJECT_PACKAGE + ".rest");
    }

    @Bean
    @Primary
    public SettingsService get() {
        return () -> projectSettings;
    }
}
