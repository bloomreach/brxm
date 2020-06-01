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


import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.ResourceModifyingTest;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import static org.junit.Assert.assertEquals;

public class DirectoryInstructionTest extends ResourceModifyingTest {

    private static Map<String, Object> dummyParameters = new HashMap<>();

    @Inject private AutowireCapableBeanFactory injector;
    @Inject private ProjectService projectService;

    @Test
    public void test_create() throws Exception {
        createModifiableDirectory("test");

        final DirectoryInstruction instruction = new DirectoryInstruction();
        injector.autowireBean(instruction);
        instruction.setAction("create");
        instruction.setTarget(getTargetPath().toString());
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(dummyParameters));
    }

    @Test
    public void testCopy() throws Exception {
        createModifiableDirectory("test");

        final DirectoryInstruction instruction = new DirectoryInstruction();
        injector.autowireBean(instruction);
        assertEquals(Instruction.Status.FAILED, instruction.execute(dummyParameters));

        instruction.setSource("/instructions");
        instruction.setAction("copy");
        instruction.setTarget(getTargetPath().toString());
        assertEquals(Instruction.Status.FAILED, instruction.execute(dummyParameters)); // Fails because unit test is not running in JAR
    }

    private Path getTargetPath() {
        return projectService.getBasePathForModule(Module.PROJECT).resolve("instructions");
    }
}
