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

import java.util.function.BiConsumer;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Strings;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Execute instruction instantiates and executes a custom Instruction class.
 */
@Component
@XmlRootElement(name = "execute", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class ExecuteInstruction extends BuiltinInstruction {

    private static final Logger log = LoggerFactory.getLogger(ExecuteInstruction.class);

    private String clazz;

    public ExecuteInstruction() {
        super(MessageGroup.EXECUTE);
    }

    @XmlAttribute(name = "class")
    public String getClazz() {
        return clazz;
    }

    public void setClazz(final String clazz) {
        this.clazz = clazz;
    }

    @Override
    public InstructionStatus execute(final PluginContext context) {
        if (Strings.isNullOrEmpty(clazz)) {
            log.warn("Cannot execute instruction, class name was not defined");
            return InstructionStatus.FAILED;
        }
        final Instruction instruction = GlobalUtils.newInstance(clazz);
        log.info("Executing instruction '{}'.", clazz);
        return instruction.execute(context);
    }

    @Override
    void populateDefaultChangeMessages(final BiConsumer<MessageGroup, String> changeMessageQueue) {
        final Instruction instruction = GlobalUtils.newInstance(clazz);
        final BooleanWrapper signal = new BooleanWrapper();
        if (instruction != null) {
            instruction.populateChangeMessages((g, m) -> {
                changeMessageQueue.accept(g, m);
                signal.flag = true;
            });
        }
        if (!signal.flag) {
            changeMessageQueue.accept(getDefaultGroup(), "Execute instruction class '" + clazz + "'.");
        }
    }

    private static class BooleanWrapper {
        boolean flag;
    }
}
