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

package org.onehippo.cms7.essentials.dashboard.instruction;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.GuiceJUnitModules;
import org.onehippo.cms7.essentials.GuiceJUnitRunner;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.utils.inject.EventBusModule;
import org.onehippo.cms7.essentials.dashboard.utils.inject.PropertiesModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitModules({EventBusModule.class, PropertiesModule.class})
public class XmlInstructionTest extends BaseRepositoryTest{

    private static Logger log = LoggerFactory.getLogger(XmlInstructionTest.class);

    @Inject
    private InstructionExecutor executor;

    @Inject
    private XmlInstruction addNodeInstruction;

    @Test
    public void testInstructions() throws Exception {

        final Node rootNode = session.getRootNode();
        log.info("rootNode {}", rootNode.getPath());
        addNodeInstruction.setAction(PluginInstruction.COPY);
        addNodeInstruction.setTarget("/");
        addNodeInstruction.setSource("/instruction_xml_file.xml");
        final InstructionSet set = new PluginInstructionSet();
        set.addInstruction(addNodeInstruction);
        executor.execute(set, getContext());



    }
}
