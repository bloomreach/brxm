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

import java.io.InputStream;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.event.listeners.InstructionsEventListener;
import org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instruction.parser.InstructionParser;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */

public class PluginInstructionExecutorTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(PluginInstructionExecutorTest.class);
    @Inject
    private InstructionsEventListener listener;
    @Inject
    private PluginInstructionExecutor pluginInstructionExecutor;
    @Inject
    private InstructionParser instructionParser;

    @Test
    public void testExecute() throws Exception {

        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("instructions.xml");
        final String content = GlobalUtils.readStreamAsText(resourceAsStream);
        log.info("content {}", content);
        listener.reset();

        final Instructions instructions = instructionParser.parseInstructions(content);
        final Set<InstructionSet> instructionSets = instructions.getInstructionSets();
        for (InstructionSet instructionSet : instructionSets) {
            pluginInstructionExecutor.execute(instructionSet, getContext());
        }

        // we had 6 executed, see /instructions.xml, 2 file and 2 XML instructions and 1 folder + default group (folder)
        assertEquals(6, listener.getNrInstructions());

    }

    @Test
    public void testExecuteByGroup() throws Exception {

        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("instructions.xml");
        final String content = GlobalUtils.readStreamAsText(resourceAsStream);
        log.info("content {}", content);
        listener.reset();

        final Instructions instructions = instructionParser.parseInstructions(content);
        final Set<InstructionSet> instructionSets = instructions.getInstructionSets();
        for (InstructionSet instructionSet : instructionSets) {
            if (instructionSet.getGroup().equals("myGroup")) {
                pluginInstructionExecutor.execute(instructionSet, getContext());
            }
        }

        // we had 5 executed, see /instructions.xml, 2 file and 2 XML instructions and 1 folder
        assertEquals(5, listener.getNrInstructions());
        // default group:
        listener.reset();
        for (InstructionSet instructionSet : instructionSets) {
            if (instructionSet.getGroup().equals(EssentialConst.INSTRUCTION_GROUP_DEFAULT)) {
                pluginInstructionExecutor.execute(instructionSet, getContext());
            }
        }

        // default group has only 1 instruciton
        assertEquals(1, listener.getNrInstructions());


    }
}
