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

package org.onehippo.cms7.essentials.dashboard.instruction.executors;

import java.io.InputStream;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionParser;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;

import static org.junit.Assert.assertEquals;

public class MessageInstructionExecutorTest extends BaseTest{

    @Inject
    private InstructionParser parser;
    @Test
    public void testParseInstructionSet() throws Exception {
        MessageInstructionExecutor executor = new MessageInstructionExecutor();
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("parser_instructions.xml");
        final String content = GlobalUtils.readStreamAsText(resourceAsStream);
        final Instructions instructions = parser.parseInstructions(content);
        final Set<InstructionSet> instructionSets = instructions.getInstructionSets();
        assertEquals(3, instructionSets.size());
        final Set<KeyValueRestful> messages = executor.execute(instructionSets, getContext());
        assertEquals(7, messages.size());



    }
}