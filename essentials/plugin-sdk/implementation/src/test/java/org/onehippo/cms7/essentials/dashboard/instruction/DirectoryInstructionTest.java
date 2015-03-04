/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.After;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.FileUtils;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.junit.Assert.assertTrue;

public class DirectoryInstructionTest extends BaseResourceTest {

    public static final String FIRST = "bar";
    public static final String SECOND = "foo";
    public static final String THIRD = "EssentialsDirectoryInstructionTest";
    public static final String DIR = File.separator + THIRD + File.separator + SECOND + File.separator + FIRST;

    @Inject
    private InstructionExecutor executor;
    @Inject
    @Qualifier("directoryInstruction")
    private DirectoryInstruction createInstruction;
    @Inject
    @Qualifier("directoryInstruction")
    private DirectoryInstruction deleteInstruction;

    private static String createPlaceHolder(final String placeholderProjectRoot) {
        return "{{" + placeholderProjectRoot + "}}";
    }

    @Test
    public void testProcess() throws Exception {

        final InstructionSet set = new PluginInstructionSet();
        set.addInstruction(createInstruction);
        InstructionStatus status = executor.execute(set, getContext());
        // invalid instruction:
        assertTrue(status == InstructionStatus.FAILED);
        createInstruction.setAction("create");
        createInstruction.setTarget(System.getProperty("java.io.tmpdir") + DIR);
        status = executor.execute(set, getContext());
        assertTrue(status == InstructionStatus.SUCCESS);

    }

    @Override
    @After
    public void tearDown() throws Exception {
        boolean deleted = FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpdir") + DIR));
        assertTrue(deleted);
        deleted = FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpdir") + File.separator + THIRD + File.separator + SECOND));
        assertTrue(deleted);
        deleted = FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpdir") + File.separator + THIRD));
        assertTrue(deleted);
    }
}
