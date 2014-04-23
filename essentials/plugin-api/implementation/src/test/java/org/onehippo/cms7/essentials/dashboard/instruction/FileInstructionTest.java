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

import java.io.File;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class FileInstructionTest extends BaseResourceTest {


    public static final String SOURCE = createPlaceHolder(EssentialConst.PLACEHOLDER_PROJECT_ROOT) + "/instruction_file.txt";
    public static final String TARGET = createPlaceHolder(EssentialConst.PLACEHOLDER_PROJECT_ROOT) + "/instruction_file_copy.txt";
    @Inject
    private InstructionExecutor executor;
    @Inject
    @Qualifier("fileInstruction")
    private FileInstruction copyInstruction;
    @Inject
    @Qualifier("fileInstruction")
    private FileInstruction deleteInstruction;

    private static String createPlaceHolder(final String placeholderProjectRoot) {
        return "{{" + placeholderProjectRoot + "}}";
    }

    @Test
    public void testProcess() throws Exception {

        final InstructionSet set = new PluginInstructionSet();
        set.addInstruction(copyInstruction);
        InstructionStatus status = executor.execute(set, getContext());
        // invalid instruction:
        assertTrue(status == InstructionStatus.FAILED);
        copyInstruction.setAction(PluginInstruction.COPY);
        copyInstruction.setSource(SOURCE);
        copyInstruction.setTarget(TARGET);
        copyInstruction.setOverwrite(true);
        status = executor.execute(set, getContext());
        assertTrue(status == InstructionStatus.SUCCESS || status == InstructionStatus.SKIPPED);
        assertTrue(!copyInstruction.getMessage().contains("{{"));
        File file = new File(copyInstruction.getTarget());
        assertTrue(file.exists());
        StringBuilder textFile = GlobalUtils.readTextFile(file.toPath());
        assertTrue(textFile.toString().contains(BaseTest.PROJECT_NAMESPACE_TEST));
        //############################################
        // BINARY TEST (no replacements):
        //############################################
        copyInstruction.setOverwrite(true);
        copyInstruction.setBinary(true);
        executor.execute(set, getContext());
        file = new File(copyInstruction.getTarget());
        assertTrue(file.exists());
        textFile = GlobalUtils.readTextFile(file.toPath());
        assertTrue(textFile.toString().contains("{{namespace}}"));

        //############################################
        // DELETE
        //############################################
        deleteInstruction.setAction(PluginInstruction.DELETE);
        deleteInstruction.setTarget(TARGET);
        final InstructionSet deleteSet = new PluginInstructionSet();
        deleteSet.addInstruction(deleteInstruction);
        status = executor.execute(deleteSet, getContext());
        assertTrue(status == InstructionStatus.SUCCESS);
        assertTrue(!deleteInstruction.getMessage().contains("{{"));


    }
}
