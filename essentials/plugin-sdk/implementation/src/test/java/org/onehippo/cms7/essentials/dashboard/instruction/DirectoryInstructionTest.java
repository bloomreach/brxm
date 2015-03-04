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
