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

package org.onehippo.cms7.essentials.dashboard.event;

import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;

/**
 * @version "$Id$"
 */
public class InstructionEvent extends MessageEvent {

    private static final long serialVersionUID = 1L;
    private Instruction instruction;

    public InstructionEvent(final Instruction instruction) {
        super(instruction.getMessage());
        this.instruction = instruction;
    }

    public InstructionEvent(final String message) {
        super(message);
    }

    @Override
    public DisplayLocation getDisplayLocation() {
        return DisplayLocation.SYSTEM;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("InstructionEvent{");
        sb.append("instruction=").append(instruction);
        sb.append('}');
        return sb.toString();
    }
}
