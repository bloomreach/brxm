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

package org.onehippo.cms7.essentials.dashboard.blog;

import java.io.File;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.InstructionPackage;
import org.onehippo.cms7.essentials.dashboard.packaging.TemplateSupportInstructionPackage;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * @version "$Id$"
 */
public class BlogInstructionPackageTest extends BaseRepositoryTest {

    @Inject
    private AutowireCapableBeanFactory injector;

    private File jspDirectory;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        jcrService.registerNodeTypes("/test-hippofacnav.cnd");
        jcrService.registerNodeTypes("/test-selection-types.cnd");
        jspDirectory = new File(getContext().getPlaceholderData().get(EssentialConst.PLACEHOLDER_JSP_ROOT)
                + File.separator + "essentials" + File.separator +"blog");
        final PluginContext context = getContext();
        jcrService.createHstRootConfig(context.getProjectNamespacePrefix());
    }

    @Test
    public void testExecute() throws Exception {
        final InstructionPackage instructionPackage = new TemplateSupportInstructionPackage("/META-INF/blog_instructions.xml");
        injector.autowireBean(instructionPackage);
        final InstructionStatus status = instructionPackage.execute(getContext());
        // mm: todo check why skipped is returned (one of the instructions is skipped)
        //assertEquals(InstructionStatus.SKIPPED, status);
        // TODO fix jspDir
        //assertEquals(TOTAL_FILES, jspDirectory.listFiles().length);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        // delete all tmp files:

        if (jspDirectory != null && jspDirectory.exists()) {
            FileUtils.deleteDirectory(jspDirectory);
        }
    }
}
