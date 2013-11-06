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

package org.onehippo.cms7.essentials.powerpack;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.packaging.PowerpackPackage;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class BasicPowerpackTest extends BaseTest {

    private static Logger log = LoggerFactory.getLogger(BasicPowerpackTest.class);
    @Inject
    private EventBus eventBus;
    private File jspDirectory;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        jspDirectory = new File(getContext().getPlaceholderData().get(EssentialConst.PLACEHOLDER_TMP_FOLDER) + File.separator + "essentials");
    }

    @Test
    public void testParseInstructions() throws Exception {
        final PowerpackPackage powerpackPackage = new BasicPowerpack();
        final Instructions instructions = powerpackPackage.getInstructions();
        assertEquals(2, instructions.getInstructionSets().size());
    }

    @Test
    public void testExecute() throws Exception {
        final PowerpackPackage powerpackPackage = new BasicPowerpack();
        final InstructionStatus status = powerpackPackage.execute(getContext());
        assertEquals(InstructionStatus.SUCCESS, status);
        assertEquals(jspDirectory.listFiles().length, 6);


    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        // delete all tmp files:

        if (jspDirectory != null && jspDirectory.exists()) {
            FileUtils.deleteDirectory(jspDirectory);
        }

    }
}
