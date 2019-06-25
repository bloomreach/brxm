/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.packaging;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructions;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class DefaultInstructionPackageTest extends BaseTest {

    @Test
    public void testInstructionPackageNoInjection() {
        try {
            DefaultInstructionPackage instructionPackage = injector.createBean(DefaultInstructionPackage.class);
            instructionPackage.execute(new HashMap<>());
        } catch (RuntimeException e) {
            fail("No RuntimeException should occur.");
        }
    }

    @Test
    public void testInstructionPackage() {
        DefaultInstructionPackage instructionPackage = injector.createBean(DefaultInstructionPackage.class);
        injector.autowireBean(instructionPackage);
        final String instructionPath = instructionPackage.getInstructionPath();
        assertEquals(instructionPath, EssentialConst.DEFAULT_INSTRUCTIONS_PATH);
        
        final PluginInstructions instructions = instructionPackage.getInstructions();
        assertNull(instructions);
    }
}
