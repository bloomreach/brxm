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

package org.onehippo.cms7.essentials.sdk.api.install;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Contract for implementing a plugin installation instruction.
 *
 * All instructions should describe their (intended) action by means of one or more "change messages".
 *
 * If an Essentials Plugin implements an Instruction, to be executed during the "setup" phase of the installation
 * by means of an &lt;execute class="fqcn.of.custom.Instruction"/&gt; element in the Plugin's package file,
 * the class-instance will be @Inject-ed with beans known to the Spring application, such that services
 * exposed through the SDK API can be used in these instructions.
 */
public interface Instruction {

    /**
     * Enumeration of the instruction type. Used to group change messages of a plugin.
     */
    enum Type {
        FILE_CREATE,
        FILE_DELETE,
        XML_NODE_CREATE,
        XML_NODE_DELETE,
        XML_NODE_FOLDER_DELETE,
        XML_NODE_FOLDER_CREATE,
        DOCUMENT_REGISTER,
        EXECUTE,
        UNKNOWN,
    }

    /**
     * Result of executing an instruction. If the instruction determines that it is superfluous, it shall return SKIPPED.
     */
    enum Status {
        SUCCESS, FAILED, SKIPPED
    }

    /**
     * Execute this instruction.
     *
     * @param parameters provides access to built-in and custom execution parameters
     * @return result of the instruction execution (success, failure, skipped)
     */
    Status execute(Map<String, Object> parameters);

    /**
     * Ask the instruction to declare its contribution to the "Changes made by this feature".
     *
     * An instruction can push 0 or more messages. Each message pertains to a {@link Type},
     * and instruction-specific variables should be interpolated before pushing. Each change message will
     * be post-processed in order to interpolate project-specific variables.
     *
     * @param changeMessageQueue 'consumer' to push 0 or more [messageGroup, changeMessage] tuples onto.
     */
    default void populateChangeMessages(BiConsumer<Type, String> changeMessageQueue) { }
}
