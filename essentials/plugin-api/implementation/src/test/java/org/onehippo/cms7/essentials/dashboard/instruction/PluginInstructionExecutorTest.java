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

import java.io.InputStream;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.TestPluginContext;
import org.onehippo.cms7.essentials.dashboard.event.listeners.InstructionsEventListener;
import org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instruction.parser.InstructionParser;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.inject.EventBusModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class PluginInstructionExecutorTest extends BaseTest{

    private static Logger log = LoggerFactory.getLogger(PluginInstructionExecutorTest.class);


    @Inject
    private InstructionsEventListener listener;


    @Inject
    private PluginInstructionExecutor pluginInstructionExecutor;

    @Test
    public void testExecute() throws Exception {

        final InputStream resourceAsStream = getClass().getResourceAsStream("/instructions.xml");
        final StringBuilder myBuilder = GlobalUtils.readStreamAsText(resourceAsStream);
        final String content = myBuilder.toString();
        log.info("content {}", content);
        final Instructions instructions = InstructionParser.parseInstructions(content);
        final Set<InstructionSet> instructionSets = instructions.getInstructionSets();
        for (InstructionSet instructionSet : instructionSets) {
            pluginInstructionExecutor.execute(instructionSet, getContext());
        }

        // we had 3 executed
        assertEquals(3, listener.getNrInstructions());

    }
}
