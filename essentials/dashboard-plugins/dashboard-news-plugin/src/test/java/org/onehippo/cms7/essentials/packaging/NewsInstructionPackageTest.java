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

package org.onehippo.cms7.essentials.packaging;

import java.io.File;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.packaging.InstructionPackage;
import org.onehippo.cms7.essentials.dashboard.packaging.TemplateSupportInstructionPackage;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class NewsInstructionPackageTest extends BaseRepositoryTest {


    public static final int TOTAL_INSTRUCTIONS = 11;



    private File jspDirectory;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        jspDirectory = new File(getContext().getPlaceholderData().get(EssentialConst.PLACEHOLDER_JSP_ROOT) + File.separator + "essentials");
        createHstRootConfig();

    }

    @Test
    public void testParseInstructions() throws Exception {
        final InstructionPackage instructionPackage = new TemplateSupportInstructionPackage("/META-INF/news_package_instructions.xml");
        injector.autowireBean(instructionPackage);
        final Instructions instructions = instructionPackage.getInstructions();
        final Set<InstructionSet> instructionSets = instructions.getInstructionSets();
        assertEquals(TOTAL_INSTRUCTIONS, instructionSets.size());
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
