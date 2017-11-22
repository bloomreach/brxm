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

package org.onehippo.cms7.essentials.dashboard.instruction;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.collect.Multimap;

import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;

/**
 * Instructions built into Essentials (such that they can be used through a corresponding element in a plugin's
 * "instructions.xml" file) should extend from BuiltinInstruction, such that they all share the optional "message"
 * attribute. When provided, this attribute's value shall override the instruction's default change message (see
 * #getChangeMessages).
 *
 * TODO: this class should be moved out of the plugin sdk and into the dashboard.
 */
public abstract class BuiltinInstruction implements Instruction {
    private final MessageGroup defaultGroup;
    private String message;

    BuiltinInstruction(final MessageGroup defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    @XmlAttribute
    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    @XmlTransient
    protected MessageGroup getDefaultGroup() {
        return defaultGroup;
    }

    @Override
    public Multimap<MessageGroup, String> getChangeMessages() {
        String message = getMessage();
        if (message != null) {
            return Instruction.makeChangeMessages(defaultGroup, message);
        }

        return getDefaultChangeMessages();
    }

    /**
     * Implement this method to provide sensible default messages per subclass.
     */
    protected abstract Multimap<MessageGroup, String> getDefaultChangeMessages();
}
