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

package org.onehippo.cms7.essentials.dashboard.instructions;

import java.util.function.BiConsumer;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;

/**
 * Contract for implementing a plugin installation instruction.
 *
 * All instructions should describe their (intended) action by means of one or more "change messages".
 */
public interface Instruction {

    /**
     * Execute this instruction.
     *
     * @param context {@link PluginContext} providing access to the project sources and repository through Essentials'
     *                                     services
     * @return result of the instruction execution (success, failure, skipped)
     */
    InstructionStatus execute(PluginContext context);

    /**
     * Ask the instruction to declare its contribution to the "Changes made by this feature".
     *
     * An instruction can push 0 or more messages. Each message pertains to a {@link MessageGroup},
     * and instruction-specific variables should be interpolated before pushing. Each change message will
     * be post-processed in order to interpolate project-specific variables.
     *
     * @param changeMessageQueue 'consumer' to push 0 or more [messageGroup, changeMessage] tuples onto.
     */
    default void populateChangeMessages(BiConsumer<MessageGroup, String> changeMessageQueue) { }
}
