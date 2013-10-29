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
import org.junit.runner.RunWith;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.GuiceJUnitModules;
import org.onehippo.cms7.essentials.GuiceJUnitRunner;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.inject.EventBusModule;
import org.onehippo.cms7.essentials.dashboard.utils.inject.PropertiesModule;

import com.google.inject.Inject;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class FileInstructionTest extends BaseTest {

    public static final String SOURCE = createPlaceHolder(EssentialConst.PLACEHOLDER_PROJECT_ROOT) + "/instruction_file.txt";
    public static final String TARGET = createPlaceHolder(EssentialConst.PLACEHOLDER_PROJECT_ROOT) + "/instruction_file_copy.txt";

    @Inject
    InstructionExecutor executor;
    @Inject
    FileInstruction instruction;

    private static String createPlaceHolder(final String placeholderProjectRoot) {
        return "${" + placeholderProjectRoot + '}';
    }

    @Test
    public void testProcess() throws Exception {
        instruction.setAction("copy");
        instruction.setSource(SOURCE);
        instruction.setTarget(TARGET);
        final InstructionStatus status = executor.execute(instruction, getContext());
        assertTrue(status == InstructionStatus.SUCCESS);
    }
}
