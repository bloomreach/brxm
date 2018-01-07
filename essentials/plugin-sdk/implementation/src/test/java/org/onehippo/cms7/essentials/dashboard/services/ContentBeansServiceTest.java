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

package org.onehippo.cms7.essentials.dashboard.services;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.UserFeedback;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

@Ignore("Needs running hippo repository through RMI")
public class ContentBeansServiceTest extends BaseRepositoryTest {

    private ContentBeansService contentBeansService = new ContentBeansServiceImpl();

    @Test
    public void testCreateBeans() throws Exception {
        final PluginContext context = getContext();
        final UserFeedback feedback = new UserFeedback();
        context.addPlaceholderData(EssentialConst.INSTRUCTION_UPDATE_IMAGE_SETS, "true");
        final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        jcrService.setHippoRepository(repository);
        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, "/home/machak/java/projects/hippo/testproject");
        contentBeansService.createBeans(jcrService, context, feedback, null);
        contentBeansService.convertImageMethods("testproject:testasasasas", context, feedback);
    }
}