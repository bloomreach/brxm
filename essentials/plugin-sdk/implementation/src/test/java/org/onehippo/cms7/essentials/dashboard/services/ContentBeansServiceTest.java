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

package org.onehippo.cms7.essentials.dashboard.services;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.TestPluginContext;
import org.onehippo.cms7.essentials.dashboard.model.UserFeedback;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

@Ignore("Needs running hippo repository through RMI")
public class ContentBeansServiceTest extends BaseRepositoryTest {

    private ContentBeansService contentBeansService = new ContentBeansServiceImpl();

    @Test
    public void testCreateBeans() throws Exception {

        final TestPluginContext context = getTestContext();
        final UserFeedback feedback = new UserFeedback();
        context.addPlaceholderData(EssentialConst.INSTRUCTION_UPDATE_IMAGE_SETS, "true");
        final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        context.setUseHippoSession(true);
        context.setHippoRepository(repository);
        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, "/home/machak/java/projects/hippo/testproject");
        contentBeansService.createBeans(context, feedback, null);
        contentBeansService.convertImageMethods("testproject:testasasasas", context, feedback);

    }

    private TestPluginContext getTestContext() {

        final TestPluginContext testPluginContext = new TestPluginContext(repository);
        testPluginContext.setComponentsPackageName("org.example.components");
        testPluginContext.setBeansPackageName("org.example.beans");
        testPluginContext.setRestPackageName("org.example.rest");
        testPluginContext.setProjectNamespacePrefix("testproject");
        testPluginContext.setProjectPackageName("org.example");

        return testPluginContext;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        projectSetup();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}