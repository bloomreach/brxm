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
import java.util.function.BiConsumer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructionSet;
import org.onehippo.cms7.essentials.plugin.sdk.install.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.model.Restful;
import org.onehippo.cms7.essentials.plugin.sdk.rest.MessageRestful;
import org.onehippo.cms7.essentials.plugin.sdk.utils.TemplateUtils;

/**
 * Unlike {@code PluginInstructionExecutor}, {@code MessageInstructionExecutor} does not execute any instructions,
 * but returns set of messages. Those can be used and presented to user before we execute anything.
 *
 * @version "$Id$"
 * @see PluginInstructionExecutor
 */
public class MessageInstructionExecutor {

    public Multimap<Instruction.Type, Restful> execute(final PluginInstructionSet instructionSet, PluginContext context) {
        final Multimap<Instruction.Type, Restful> changeMessages = ArrayListMultimap.create();
        final Map<String, Object> placeholderData = context.getPlaceholderData();
        final BiConsumer<Instruction.Type, String> changeMessageCollector
                = (g, m) -> changeMessages.put(g, new MessageRestful(TemplateUtils.replaceTemplateData(m, placeholderData)));

        instructionSet.getInstructions().forEach(i -> i.populateChangeMessages(changeMessageCollector));

        return changeMessages;
    }
}
