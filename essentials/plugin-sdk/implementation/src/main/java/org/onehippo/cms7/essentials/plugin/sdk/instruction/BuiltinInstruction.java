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

package org.onehippo.cms7.essentials.plugin.sdk.instruction;

import java.util.function.BiConsumer;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;

/**
 * Instructions built into Essentials (such that they can be used through a corresponding element in a plugin's
 * "instructions.xml" file) should extend from BuiltinInstruction, such that they all share the optional "message"
 * attribute. When provided, this attribute's value shall override the instruction's default change message (see
 * #populateChangeMessages).
 */
public abstract class BuiltinInstruction implements Instruction {
    private final Type defaultGroup;
    private String message;

    BuiltinInstruction(final Type defaultGroup) {
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
    Type getDefaultGroup() {
        return defaultGroup;
    }

    @Override
    public final void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        String message = getMessage();
        if (message != null) {
            changeMessageQueue.accept(defaultGroup, message);
        } else {
            populateDefaultChangeMessages(changeMessageQueue);
        }
    }

    /**
     * Implement this method to provide sensible default messages per subclass.
     */
    abstract void populateDefaultChangeMessages(BiConsumer<Type, String> changeMessageQueue);
}
