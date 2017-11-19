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

package org.onehippo.cms7.essentials.plugins.robots;

import com.google.common.collect.Multimap;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.utils.WebXmlUtils;

/**
 * Add a bean mapping to the Site's web.xml file.
 */
public class RobotsInstruction implements Instruction {

    private static final String BEANS_MAPPING = "classpath*:org/onehippo/forge/**/*.class";

    @Override
    public InstructionStatus execute(final PluginContext context) {
        return WebXmlUtils.addHstBeanMapping(context, BEANS_MAPPING) ? InstructionStatus.SUCCESS : InstructionStatus.FAILED;
    }

    @Override
    public Multimap<MessageGroup, String> getChangeMessages() {
        return Instruction.makeChangeMessages(MessageGroup.EXECUTE,
                "Add mapping '" + BEANS_MAPPING + "' for annotated HST beans to Site web.xml.");
    }
}
