/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class NodeFolderInstructionTest extends BaseRepositoryTest {


    @Inject
    private NodeFolderInstruction instruction;
    @Inject
    private InstructionExecutor executor;

    @Test
    public void testInstruction() throws Exception {
        instruction.setPath("/foo/bar/foobar");
        instruction.setTemplate("/my_folder_template.xml");
        final InstructionSet instructionSet = new PluginInstructionSet();
        instructionSet.addInstruction(instruction);
        final InstructionStatus execute = executor.execute(instructionSet, getContext());
        assertEquals(execute, InstructionStatus.SUCCESS);


    }
}
