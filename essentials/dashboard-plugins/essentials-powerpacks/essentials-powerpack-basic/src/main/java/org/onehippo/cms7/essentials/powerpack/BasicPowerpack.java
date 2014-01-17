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

package org.onehippo.cms7.essentials.powerpack;

import java.io.InputStream;
import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instruction.parser.InstructionParser;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.packaging.PowerpackPackage;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
public class BasicPowerpack implements PowerpackPackage {

    private static Logger log = LoggerFactory.getLogger(BasicPowerpack.class);

    private Instructions instructions;
    @Inject
    private EventBus eventBus;

    @Override
    public Instructions getInstructions() {
        if (instructions == null) {
            final InputStream resourceAsStream = getClass().getResourceAsStream("/META-INF/instructions.xml");
            final String content = GlobalUtils.readStreamAsText(resourceAsStream);
            instructions = InstructionParser.parseInstructions(content);
        }
        return instructions;

    }

    @Override
    public InstructionStatus execute(final PluginContext context) {
        if (instructions == null) {
            getInstructions();
        }
        if (instructions == null) {
            eventBus.post(new DisplayEvent("Couldn't parse instructions"));
            log.error("Failed to parse instructions");
            return InstructionStatus.FAILED;
        }
        final Set<InstructionSet> instructionSets = instructions.getInstructionSets();
        InstructionStatus status = InstructionStatus.SUCCESS;
        final InstructionExecutor executor = new PluginInstructionExecutor();
        for (InstructionSet instructionSet : instructionSets) {
            // currently we return fail if any of instructions is failed
            if (status == InstructionStatus.FAILED) {
                executor.execute(instructionSet, context);
                continue;
            }
            status = executor.execute(instructionSet, context);
        }
        // TODO
        return status;
    }

}
