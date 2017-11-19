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

import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;

/**
 * Contract for implementing a plugin installation instruction.
 *
 * All instructions should describe their (intended) action by means of one or more "change messages".
 */
@XmlTransient
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
     * Retrieve the "Changes made by this feature" messages.
     *
     * An instruction can return 1 or more messages. Each message must be assigned to a {@link MessageGroup},
     * and instruction-specific variables should be interpolated before returning. Each change message will
     * be post-processed in order to interpolate project-specific variables.
     *
     * @return a {@link Multimap} containing all change messages.
     */
    default Multimap<MessageGroup, String> getChangeMessages() {
        return null;
    }

    /**
     * Convenience method for building a change messages multimap.
     */
    static Multimap<MessageGroup, String> makeChangeMessages(final MessageGroup group, final String... messages) {
        final Multimap<MessageGroup, String> result = ArrayListMultimap.create();
        for (String message : messages) {
            result.put(group, message);
        }
        return result;
    }
}
