/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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


import java.io.File;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.plugin.sdk.instructions.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialsFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.junit.Assert.assertTrue;

public class DirectoryInstructionTest extends BaseResourceTest {

    public static final String FIRST = "bar";
    public static final String SECOND = "foo";
    public static final String THIRD = "EssentialsDirectoryInstructionTest";
    public static final String DIR = File.separator + THIRD + File.separator + SECOND + File.separator + FIRST;

    private static final Logger log = LoggerFactory.getLogger(DirectoryInstructionTest.class);
    @Inject private PluginInstructionExecutor executor;
    @Inject
    @Qualifier("directoryInstruction")
    private DirectoryInstruction createInstruction;
    @Inject
    @Qualifier("directoryInstruction")
    private DirectoryInstruction copyInstruction;

    private File targetDir;

    private static String createPlaceHolder(final String placeholderProjectRoot) {
        return "{{" + placeholderProjectRoot + "}}";
    }

    @Test
    public void testProcess() throws Exception {

        final PluginInstructionSet set = new PluginInstructionSet();
        set.addInstruction(createInstruction);
        Instruction.Status status = executor.execute(set, getContext());
        // invalid instruction:
        assertTrue(status == Instruction.Status.FAILED);
        createInstruction.setAction("create");
        createInstruction.setTarget(System.getProperty("java.io.tmpdir") + DIR);
        status = executor.execute(set, getContext());
        assertTrue(status == Instruction.Status.SUCCESS);

    }

    @Test
    public void testCopy() throws Exception {

        targetDir = new File(System.getProperty("java.io.tmpdir") + File.separatorChar + getClass().getSimpleName());
        log.info("testing copy to: {}", targetDir);
        final PluginInstructionSet set = new PluginInstructionSet();
        set.addInstruction(copyInstruction);
        final PluginContext context = getContext();
        Instruction.Status status = executor.execute(set, context);
        assertTrue(status == Instruction.Status.FAILED);
        copyInstruction.setSource("/");
        copyInstruction.setAction("copy");
        copyInstruction.setTarget(targetDir.getAbsolutePath());
        status = executor.execute(set, context);
        assertTrue("Expected to fail: " + status, status == Instruction.Status.FAILED);

    }

    @Override
    @After
    public void tearDown() throws Exception {
        EssentialsFileUtils.deleteSingleDirectory(new File(System.getProperty("java.io.tmpdir") + DIR));
        EssentialsFileUtils.deleteSingleDirectory(new File(System.getProperty("java.io.tmpdir") + File.separator + THIRD + File.separator + SECOND));
        EssentialsFileUtils.deleteSingleDirectory(new File(System.getProperty("java.io.tmpdir") + File.separator + THIRD));
        if (targetDir != null) {
            FileUtils.deleteDirectory(targetDir);
        }
    }
}
