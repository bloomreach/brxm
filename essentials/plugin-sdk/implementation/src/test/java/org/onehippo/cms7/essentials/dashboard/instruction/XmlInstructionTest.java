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

package org.onehippo.cms7.essentials.dashboard.instruction;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class XmlInstructionTest extends BaseRepositoryTest {

    /**
     * See instruction_xml_file.xml file
     */
    private static final String NODE_NAME = "testNode";
    @Inject private PluginInstructionExecutor executor;
    @Inject private XmlInstruction addNodeInstruction;
    @Inject private XmlInstruction removeNodeInstruction;

    @Test
    public void testInstructions() throws Exception {

        //############################################
        // ADD
        //############################################
        addNodeInstruction.setActionEnum(XmlInstruction.Action.COPY);
        addNodeInstruction.setTarget("/");
        addNodeInstruction.setSource("instruction_xml_file.xml");
        final PluginInstructionSet set = new PluginInstructionSet();
        set.addInstruction(addNodeInstruction);
        Instruction.Status status = executor.execute(set, getContext());
        assertTrue("Expected SUCCESS but got: " + status, status == Instruction.Status.SUCCESS);
        //############################################
        // OVERRIDE FALSE TEST
        //############################################
        addNodeInstruction.setOverwrite(false);
        status = executor.execute(set, getContext());
        assertTrue("Expected SKIPPED but got: " + status, status == Instruction.Status.SKIPPED);
        //############################################
        // OVERRIDE TRUE TEST: NOT SUPPORTED YET
        //############################################
        /*
        addNodeInstruction.setOverwrite(true);
        status = executor.execute(set, getContext());
        assertTrue("Expected SUCCESS but got: " + status, status == InstructionStatus.SUCCESS);
        */

        //############################################
        // DELETE
        //############################################

        removeNodeInstruction.setActionEnum(XmlInstruction.Action.DELETE);
        removeNodeInstruction.setTarget('/' + NODE_NAME);
        final PluginInstructionSet removeSet = new PluginInstructionSet();
        removeSet.addInstruction(removeNodeInstruction);
        final Instruction.Status deleteStatus = executor.execute(removeSet, getContext());
        assertTrue("Expected SUCCESS but got: " + deleteStatus, deleteStatus == Instruction.Status.SUCCESS);


    }
}
