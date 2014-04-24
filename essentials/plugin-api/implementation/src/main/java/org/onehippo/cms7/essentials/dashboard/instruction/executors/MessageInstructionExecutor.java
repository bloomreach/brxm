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

import java.util.HashSet;
import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instruction.FileInstruction;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unlike {@code PluginInstructionExecutor}, {@code MessageInstructionExecutor} does not execute any instructions,
 * but returns set of messages. Those can be used and presented to user before we execute anything.
 *
 * @version "$Id$"
 * @see org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor
 */
public class MessageInstructionExecutor {

    private static Logger log = LoggerFactory.getLogger(MessageInstructionExecutor.class);


    public Set<KeyValueRestful> execute(Iterable<InstructionSet> instructions, PluginContext context) {


        final Set<KeyValueRestful> retVal = new HashSet<>();
        for (InstructionSet instructionSet : instructions) {
            final Set<Instruction> mySet = instructionSet.getInstructions();
            for (Instruction instruction : mySet) {
                if(instruction instanceof FileInstruction){
                    FileInstruction instr = (FileInstruction) instruction;
                    log.info("instruction {}", instruction.getClass());
                    final String target = instr.getTarget();
                    final String source = instr.getSource();
                    final String action = instr.getAction();
                    final String userMessage = "";

                    KeyValueRestful keyValueRestful = new KeyValueRestful("FileInstruction", userMessage);
                    retVal.add(keyValueRestful);

                }
            }
        }


        return retVal;
    }

}
