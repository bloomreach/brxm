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

package org.onehippo.cms7.essentials.plugin.sdk.instruction.executors;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;

import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructionSet;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.springframework.stereotype.Component;

/**
 * @version "$Id$"
 */
@Component
public class PluginInstructionExecutor {

    public Instruction.Status execute(final PluginInstructionSet set, final Map<String, Object> parameters) {
        Instruction.Status status = Instruction.Status.SUCCESS;
        final Set<Instruction> instructions = set.getInstructions();
        for (Instruction instruction : instructions) {
            final Instruction.Status sts = instruction.execute(parameters);
            if (sts == Instruction.Status.FAILED) {
                status = sts;
            }
        }
        return status;
    }

    public void getInstructionsMessages(final PluginInstructionSet instructionSet,
                                        final Multimap<Instruction.Type, String> messageMap) {
        instructionSet.getInstructions().forEach(i -> i.populateChangeMessages(messageMap::put));
    }
}
