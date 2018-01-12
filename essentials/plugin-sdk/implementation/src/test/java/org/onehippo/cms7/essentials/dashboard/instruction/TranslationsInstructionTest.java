/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus.SUCCESS;

public class TranslationsInstructionTest extends BaseRepositoryTest {

    @Inject private PluginInstructionExecutor executor;
    @Inject private TranslationsInstruction translationsInstruction;

    @Test
    public void testInstruction() throws Exception {
        translationsInstruction.setSource("/instruction_translations_file.json");
        final PluginInstructionSet set = new PluginInstructionSet();
        set.addInstruction(translationsInstruction);
        final InstructionStatus status = executor.execute(set, getContext());
        assertEquals(SUCCESS, status);
    }

}
