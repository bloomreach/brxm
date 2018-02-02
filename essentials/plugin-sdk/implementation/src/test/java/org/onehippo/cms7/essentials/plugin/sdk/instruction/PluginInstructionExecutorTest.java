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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.parser.DefaultInstructionParser;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */

public class PluginInstructionExecutorTest extends BaseRepositoryTest {

    private static Map<String, Object> dummyParameters = new HashMap<>();

    @Inject private PluginInstructionExecutor pluginInstructionExecutor;
    @Inject private DefaultInstructionParser instructionParser;

    @Test
    public void testExecute() throws Exception {

        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("parser_instructions.xml");
        final String content = GlobalUtils.readStreamAsText(resourceAsStream);

        final PluginInstructions instructions = instructionParser.parseInstructions(content);
        final Set<PluginInstructionSet> instructionSets = instructions.getInstructionSets();
        assertEquals(2, instructionSets.size());
        for (PluginInstructionSet instructionSet : instructionSets) {
            pluginInstructionExecutor.execute(instructionSet, dummyParameters);
        }
    }
}
