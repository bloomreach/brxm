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

package org.onehippo.cms7.essentials.plugin.sdk.instruction;

import java.io.File;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.install.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.TemplateUtils;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileInstructionTest extends ResourceModifyingTest {

    @Test
    public void testProcess() throws Exception {
        final PluginContext context = getContext();
        final String targetInput = "{{projectRoot}}/file_instruction_copy.txt";
        final String target = TemplateUtils.replaceTemplateData(targetInput, context.getPlaceholderData());
        final File file = new File(target);
        final FileInstruction instruction = new FileInstruction();
        autoWire(instruction);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(FileInstruction.class).build()) {
            assertEquals(Instruction.Status.FAILED, instruction.execute(context));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Invalid file instruction")));
        }

        // copy file and validate result
        instruction.setAction(FileInstruction.Action.COPY.toString());
        instruction.setSource("file_instruction_test.txt");
        instruction.setTarget(targetInput);
        instruction.setOverwrite(true);
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(context));
        assertTrue(file.exists());
        assertTrue(GlobalUtils.readTextFile(file.toPath()).toString().contains(BaseTest.PROJECT_NAMESPACE_TEST));

        // overwrite with binary content (no interpolation)
        instruction.setOverwrite(true);
        instruction.setBinary(true);
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(context));
        assertTrue(file.exists());
        assertTrue(GlobalUtils.readTextFile(file.toPath()).toString().contains("{{namespace}}"));

        // and delete again
        instruction.setActionEnum(FileInstruction.Action.DELETE);
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(context));
        assertFalse(file.exists());
    }
}
