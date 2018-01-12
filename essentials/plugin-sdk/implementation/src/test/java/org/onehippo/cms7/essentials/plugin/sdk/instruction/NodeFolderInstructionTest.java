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

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.plugin.sdk.install.Instruction;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;

public class NodeFolderInstructionTest extends BaseRepositoryTest {

    @Inject private NodeFolderInstruction instruction;
    @Inject private PluginInstructionExecutor executor;

    @Test
    public void testInstruction() throws Exception {
        instruction.setPath("/foo/bar/foobar");
        instruction.setTemplate("my_folder_template.xml");
        final PluginInstructionSet instructionSet = new PluginInstructionSet();
        instructionSet.addInstruction(instruction);
        Instruction.Status execute = executor.execute(instructionSet, getContext());
        assertEquals(Instruction.Status.SUCCESS, execute);
        // should skip second time
        execute = executor.execute(instructionSet, getContext());
        assertEquals(Instruction.Status.SKIPPED, execute);

        // should fail,  wrong path
        instruction.setPath("foo/bar/foobar");
        Log4jInterceptor.onError().deny(NodeFolderInstruction.class).run(() -> {
            assertEquals(Instruction.Status.FAILED, executor.execute(instructionSet, getContext()));
        });

        // should skip,  folder exists
        instruction.setTemplate("no_template_my_folder_template.xml");
        instruction.setPath("/foo/bar/foobar");
        execute = executor.execute(instructionSet, getContext());
        assertEquals(Instruction.Status.SKIPPED, execute);
        // should fail, no  folder exists , wrong path
        instruction.setTemplate("no_template_my_folder_template.xml");
        instruction.setPath("/foo/bar/foobar/somepath");
        execute = executor.execute(instructionSet, getContext());
        assertEquals(Instruction.Status.FAILED, execute);
    }
}
