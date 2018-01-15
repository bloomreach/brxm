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

package org.onehippo.cms7.essentials;

import javax.inject.Inject;

import org.junit.Before;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version "$Id$"
 */
@ActiveProfiles("repository-test")
public abstract class BaseRepositoryTest extends BaseTest {

    @Inject private JcrService injectedJcrService;
    protected TestJcrService jcrService;

    @Override
    public PluginContext getContext() {
        final TestPluginContext testPluginContext = new TestPluginContext();

        testPluginContext.setComponentsPackageName("org.onehippo.essentials.test.components");
        testPluginContext.setBeansPackageName("org.onehippo.essentials.test.beans");
        testPluginContext.setRestPackageName("org.onehippo.essentials.test.rest");
        testPluginContext.setProjectSettings(new ProjectSettingsBean());

        return testPluginContext;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        jcrService = (TestJcrService) injectedJcrService;
        jcrService.resetNodes();
        super.setUp();
    }
}
