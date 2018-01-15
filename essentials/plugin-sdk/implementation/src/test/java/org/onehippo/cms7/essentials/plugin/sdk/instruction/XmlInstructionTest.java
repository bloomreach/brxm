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

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.install.Instruction;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class XmlInstructionTest extends BaseRepositoryTest {

    @Inject private AutowireCapableBeanFactory injector;

    @Test
    public void testInstructions() throws Exception {
        final PluginContext context = getContext();
        final XmlInstruction instruction = new XmlInstruction();
        injector.autowireBean(instruction);

        // copy successfully
        instruction.setActionEnum(XmlInstruction.Action.COPY);
        instruction.setTarget("/");
        instruction.setSource("instruction_xml_file.xml");
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(context));

        // no override
        instruction.setOverwrite(false);
        assertEquals(Instruction.Status.SKIPPED, instruction.execute(context));

        // override (currently not supported!)
//        instruction.setOverwrite(true);
//        assertEquals(Instruction.Status.SUCCESS, instruction.execute(context));

        // delete
        instruction.setActionEnum(XmlInstruction.Action.DELETE);
        instruction.setTarget("/testNode"); // matches root node of instruction_xml_file.xml
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(context));
    }
}
