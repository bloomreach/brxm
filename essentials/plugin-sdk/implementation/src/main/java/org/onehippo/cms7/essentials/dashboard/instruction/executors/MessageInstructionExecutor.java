/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.model.Restful;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;

/**
 * Unlike {@code PluginInstructionExecutor}, {@code MessageInstructionExecutor} does not execute any instructions,
 * but returns set of messages. Those can be used and presented to user before we execute anything.
 *
 * @version "$Id$"
 * @see org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor
 */
public class MessageInstructionExecutor {

    @SuppressWarnings("InstanceofInterfaces")
    public Multimap<MessageGroup, Restful> execute(final InstructionSet instructionSet, PluginContext context) {
        final Multimap<MessageGroup, Restful> retVal = ArrayListMultimap.create();
        final Map<String, Object> placeholderData = context.getPlaceholderData();

        for (Instruction instruction : instructionSet.getInstructions()) {
            final Multimap<MessageGroup, String> changeMessages = instruction.getChangeMessages();
            if (changeMessages != null) {
                for (MessageGroup group : changeMessages.keySet()) {
                    changeMessages.get(group)
                            .stream()
                            .map(m -> TemplateUtils.replaceTemplateData(m, placeholderData))
                            .forEach(m -> retVal.put(group, new MessageRestful(m)));
                }
            }
        }

        return retVal;
    }
}
