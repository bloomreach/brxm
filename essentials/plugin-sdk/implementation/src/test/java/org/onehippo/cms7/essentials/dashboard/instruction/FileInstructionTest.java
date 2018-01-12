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

package org.onehippo.cms7.essentials.dashboard.instruction;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.TestSettingsService;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.service.ProjectService;
import org.onehippo.cms7.essentials.dashboard.services.ContentBeansService;
import org.onehippo.cms7.essentials.dashboard.services.ContentBeansServiceImpl;
import org.onehippo.cms7.essentials.dashboard.services.ProjectServiceImpl;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class FileInstructionTest extends BaseResourceTest {

    private static final String SOURCE = "file_instruction_test.txt";
    private static final String TARGET = createPlaceHolder(EssentialConst.PLACEHOLDER_PROJECT_ROOT) + "/file_instruction_copy.txt";

    @Inject private InstructionExecutor executor;

    private FileInstruction copyInstruction;
    private FileInstruction deleteInstruction;

    private static String createPlaceHolder(final String placeholderProjectRoot) {
        return "{{" + placeholderProjectRoot + "}}";
    }

    @Before
    public void setup() {
        copyInstruction = new FileInstruction();
        injector.autowireBean(copyInstruction);
        deleteInstruction = new FileInstruction();
        injector.autowireBean(deleteInstruction);
    }

    @Test
    public void testProcess() throws Exception {
        final PluginContext context = getContext();
        final String target = TemplateUtils.replaceTemplateData(TARGET, context.getPlaceholderData());

        final InstructionSet set = new PluginInstructionSet();
        set.addInstruction(copyInstruction);
        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(FileInstruction.class).build()) {
            assertEquals(InstructionStatus.FAILED, executor.execute(set, context));
            interceptor.messages().anyMatch(m -> m.contains("Invalid file instruction"));
        }

        copyInstruction.setAction(FileInstruction.Action.COPY.toString());
        copyInstruction.setSource(SOURCE);
        copyInstruction.setTarget(TARGET);
        copyInstruction.setOverwrite(true);
        assertEquals(InstructionStatus.SUCCESS, executor.execute(set, context));

        final File file = new File(target);
        assertTrue(file.exists());
        StringBuilder textFile = GlobalUtils.readTextFile(file.toPath());
        assertTrue(textFile.toString().contains(TestSettingsService.PROJECT_NAMESPACE_TEST));
        //############################################
        // BINARY TEST (no replacements):
        //############################################
        copyInstruction.setOverwrite(true);
        copyInstruction.setBinary(true);
        executor.execute(set, context);
        assertTrue(file.exists());
        textFile = GlobalUtils.readTextFile(file.toPath());
        assertTrue(textFile.toString().contains("{{namespace}}"));

        //############################################
        // DELETE
        //############################################
        deleteInstruction.setActionEnum(FileInstruction.Action.DELETE);
        deleteInstruction.setTarget(TARGET);
        final InstructionSet deleteSet = new PluginInstructionSet();
        deleteSet.addInstruction(deleteInstruction);
        assertEquals(InstructionStatus.SUCCESS, executor.execute(deleteSet, context));
    }
}
