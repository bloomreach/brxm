/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.InstructionEvent;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

/**
 * Execute instruction instantiates and executes instruction for given class.
 * NOTE: class must implement {@code org.onehippo.cms7.essentials.dashboard.instructions.Instruction}
 *
 * @version "$Id$"
 */
@Component
@XmlRootElement(name = "execute", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class ExecuteInstruction extends PluginInstruction {

    private static final Logger log = LoggerFactory.getLogger(ExecuteInstruction.class);


    @Inject
    private EventBus eventBus;

    private String message;

    private String clazz;

    @XmlAttribute
    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(final String message) {
        this.message = message;
    }

    @Override
    public String getAction() {
        return null;
    }

    @XmlAttribute(name = "class")
    public String getClazz() {
        return clazz;
    }

    public void setClazz(final String clazz) {
        this.clazz = clazz;
    }

    @Override
    public void setAction(final String action) {

    }

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        if (Strings.isNullOrEmpty(clazz)) {
            log.warn("Cannot execute instruction, class name was not defined");
            return InstructionStatus.FAILED;
        }
        final Instruction instruction = GlobalUtils.newInstance(clazz);
        eventBus.post(new InstructionEvent(instruction));
        return instruction.process(context, previousStatus);
    }
}
