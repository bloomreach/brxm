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

import javax.inject.Inject;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.plugin.sdk.install.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.services.SettingsServiceImpl;
import org.onehippo.cms7.essentials.plugin.sdk.utils.CndUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class CndInstructionTest extends BaseRepositoryTest {

    private static final String TEST_URI = "http://www.test.com";
    private static final String TEST_PREFIX = "test";

    @Inject private PluginInstructionExecutor executor;
    @Inject private CndInstruction cndInstruction;
    @Inject private SettingsServiceImpl settingsService;

    @Ignore("Ignore temporary to fix windows testcase errors")
    @Test
    public void testProcess() throws Exception {

        final Session session = jcrService.createSession();
        session.getRootNode().addNode(HippoNodeType.NAMESPACES_PATH);
        session.save();
        CndUtils.registerNamespace(jcrService, TEST_PREFIX, TEST_URI);
        CndUtils.createHippoNamespace(jcrService, TEST_PREFIX);
        boolean exists = CndUtils.namespaceUriExists(jcrService, TEST_URI);
        assertTrue(exists);

        cndInstruction.setDocumentType("newsdocument");
        settingsService.getModifiableSettings().setProjectNamespace(TEST_PREFIX);
        final PluginInstructionSet instructionSet = new PluginInstructionSet();
        instructionSet.addInstruction(cndInstruction);
        Instruction.Status status = executor.execute(instructionSet, getContext());
        assertTrue("Expected success but got: " + status, status == Instruction.Status.SUCCESS);
        // this should throw exists exception
        status = executor.execute(instructionSet, getContext());
        assertTrue("Expected failed but got: " + status, status == Instruction.Status.FAILED);
        // test prefix:
        final String testingPrefix = "testingprefix";
        cndInstruction.setNamespacePrefix(testingPrefix);
        assertEquals("testingprefix", cndInstruction.getNamespacePrefix());

        jcrService.destroySession(session);
    }
}
