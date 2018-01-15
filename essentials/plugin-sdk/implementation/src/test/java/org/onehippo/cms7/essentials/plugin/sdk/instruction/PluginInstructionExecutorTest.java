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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import javax.inject.Inject;
import javax.jcr.Session;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.parser.DefaultInstructionParser;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */

public class PluginInstructionExecutorTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(PluginInstructionExecutorTest.class);
    @Inject private PluginInstructionExecutor pluginInstructionExecutor;
    @Inject private DefaultInstructionParser instructionParser;

    @Test
    public void testExecute() throws Exception {

        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("parser_instructions.xml");
        final String content = GlobalUtils.readStreamAsText(resourceAsStream);
        log.info("content {}", content);

        final PluginInstructions instructions = instructionParser.parseInstructions(content);
        final Set<PluginInstructionSet> instructionSets = instructions.getInstructionSets();
        assertEquals(3, instructionSets.size());
        for (PluginInstructionSet instructionSet : instructionSets) {
            pluginInstructionExecutor.execute(instructionSet, getContext());
        }
    }

    @Test
    public void testExecuteByGroup() throws Exception {
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("parser_instructions.xml");
        final String content = GlobalUtils.readStreamAsText(resourceAsStream);
        final PluginInstructions instructions = instructionParser.parseInstructions(content);
        final Set<PluginInstructionSet> instructionSets = instructions.getInstructionSets();

        // execute groups labelled with "myGroup"
        for (PluginInstructionSet instructionSet : instructionSets) {
            if (instructionSet.getGroups().contains("myGroup")) {
                pluginInstructionExecutor.execute(instructionSet, getContext());
            }
        }

        // execute groups labelled with "default"
        for (PluginInstructionSet instructionSet : instructionSets) {
            if (instructionSet.getGroups().contains(EssentialConst.INSTRUCTION_GROUP_DEFAULT)) {
                pluginInstructionExecutor.execute(instructionSet, getContext());
            }
        }

        // check if date folder is created
        DateFormat formatter = new SimpleDateFormat(EssentialConst.REPO_FOLDER_FORMAT);
        final Date today = Calendar.getInstance().getTime();
        final String folder = formatter.format(today);
        final String folderPath = "/foo/bar/foobar2/" + folder;

        final Session session = jcrService.createSession();
        assertTrue(session.nodeExists(folderPath));
        jcrService.destroySession(session);
    }
}
