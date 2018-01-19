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
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NodeFolderInstructionTest extends BaseRepositoryTest {

    private static Map<String, Object> dummyParameters = new HashMap<>();

    @Inject private AutowireCapableBeanFactory injector;

    @Test
    public void testInstruction() throws Exception {
        final NodeFolderInstruction instruction = new NodeFolderInstruction();
        injector.autowireBean(instruction);

        instruction.setPath("/foo/bar/foobar");
        instruction.setTemplate("my_folder_template.xml");
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(dummyParameters));

        // should skip second time
        assertEquals(Instruction.Status.SKIPPED, instruction.execute(dummyParameters));

        // should fail, wrong path
        instruction.setPath("foo/bar/foobar");
        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(NodeFolderInstruction.class).build()) {
            assertEquals(Instruction.Status.FAILED, instruction.execute(dummyParameters));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains("Error creating folders")));
        }

        // should skip, folder exists
        instruction.setTemplate("no_template_my_folder_template.xml");
        instruction.setPath("/foo/bar/foobar");
        assertEquals(Instruction.Status.SKIPPED, instruction.execute(dummyParameters));

        // should fail, no folder exists , wrong path
        instruction.setTemplate("no_template_my_folder_template.xml");
        instruction.setPath("/foo/bar/foobar/somepath");
        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(NodeFolderInstruction.class).build()) {
            assertEquals(Instruction.Status.FAILED, instruction.execute(dummyParameters));
            assertTrue(interceptor.messages().anyMatch(m -> m.contains(
                    "Template was not found: no_template_my_folder_template.xml")));
        }
    }
}
