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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.plugin.sdk.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.plugin.sdk.utils.CndUtils;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class CndInstructionTest extends BaseRepositoryTest {

    private static final String TEST_URI = "http://www.test.com";
    private static final String TEST_PREFIX = "test";
    private static Map<String, Object> dummyParameters = new HashMap<>();

    @Inject private CndInstruction cndInstruction;

    @Test
    public void testProcess() throws Exception {
        jcrService.reset();

        // Register a 'test' namespace
        CndUtils.registerNamespace(jcrService, TEST_PREFIX, TEST_URI);
        CndUtils.createHippoNamespace(jcrService, TEST_PREFIX);
        assertTrue(CndUtils.namespaceUriExists(jcrService, TEST_URI));

        // Register a test:newsdocument node type through the CND instruction
        cndInstruction.setDocumentType("newsdocument");
        final ProjectSettingsBean projectSettings = new ProjectSettingsBean();
        projectSettings.setProjectNamespace(TEST_PREFIX);
        assertEquals(Instruction.Status.SUCCESS, cndInstruction.execute(dummyParameters));

        // trying again should 'skip'
        assertEquals(Instruction.Status.SKIPPED, cndInstruction.execute(dummyParameters));

        // test prefix setter/getter:
        cndInstruction.setNamespacePrefix("testingprefix");
        assertEquals("testingprefix", cndInstruction.getNamespacePrefix());
    }
}
