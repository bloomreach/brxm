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

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

/**
 * @version "$Id$"
 */
public class DirectoryInstructionTest extends BaseTest {


    @Inject
    private InstructionExecutor executor;
    @Inject
    private DirectoryInstruction copyInstruction;
    @Inject
    private DirectoryInstruction deleteInstruction;

    private File ourFolder = null;
    private File targetFolder = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final String tmpPath = (String) getContext().getPlaceholderData().get(EssentialConst.PLACEHOLDER_TMP_FOLDER);
        final File tmpFolder = new File(tmpPath);
        ourFolder = new File(tmpFolder.getAbsolutePath() + File.separator + getClass().getSimpleName());
        ourFolder.mkdir();
        //
        targetFolder = new File(tmpFolder.getAbsolutePath() + File.separator + getClass().getSimpleName() + "_target");

    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (ourFolder != null) {
            FileUtils.deleteDirectory(ourFolder);
        }
        if (targetFolder != null) {
            FileUtils.deleteDirectory(targetFolder);
        }
    }

    @Test
    public void testProcess() throws Exception {

        /*
        final InstructionSet set = new PluginInstructionSet();
        set.addInstruction(copyInstruction);
        InstructionStatus status = executor.execute(set, getContext());
        // invalid instruction:
        assertTrue(status == InstructionStatus.FAILED);
        copyInstruction.setAction(PluginInstruction.COPY);
        copyInstruction.setSource(ourFolder.getAbsolutePath());
        copyInstruction.setTarget(targetFolder.getAbsolutePath());
        copyInstruction.setOverwrite(true);
        status = executor.execute(set, getContext());
        assertTrue(status == InstructionStatus.SUCCESS || status == InstructionStatus.SKIPPED);
        File file = new File(copyInstruction.getTarget());
        assertTrue(file.exists());
        StringBuilder textFile = GlobalUtils.readTextFile(file.toPath());
        assertTrue(textFile.toString().contains(BaseTest.PROJECT_NAMESPACE_TEST));

        //############################################
        // DELETE
        //############################################
        deleteInstruction.setAction(PluginInstruction.DELETE);
        deleteInstruction.setTarget(targetFolder.getAbsolutePath());
        final InstructionSet deleteSet = new PluginInstructionSet();
        deleteSet.addInstruction(deleteInstruction);
        status = executor.execute(deleteSet, getContext());
        assertTrue(status == InstructionStatus.SUCCESS);
      */


    }
}
