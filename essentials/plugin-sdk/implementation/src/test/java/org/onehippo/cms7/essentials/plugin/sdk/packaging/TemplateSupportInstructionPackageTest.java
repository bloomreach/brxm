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

package org.onehippo.cms7.essentials.plugin.sdk.packaging;

import java.util.Map;
import java.util.Set;

import com.google.common.eventbus.EventBus;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructions;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.parser.DefaultInstructionParser;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @version "$Id$"
 */
public class TemplateSupportInstructionPackageTest extends BaseTest {

    @Test
    public void testInstructionPackage() throws Exception {
        DefaultInstructionPackage instructionPackage = new TemplateSupportInstructionPackage();
        injector.autowireBean(instructionPackage);
        final String instructionPath = instructionPackage.getInstructionPath();
        assertEquals("Expected default path", instructionPath, EssentialConst.DEFAULT_INSTRUCTIONS_PATH);
        final Map<String, Object> properties = instructionPackage.getProperties();
        assertEquals("Expected empty property set", 0, properties.size());
        final Set<String> groupNames = instructionPackage.groupNames();
        assertEquals("Expected default group names", 3, groupNames.size());
        final PluginInstructions instructions = instructionPackage.getInstructions();
        assertEquals("Expected no instructions", null, instructions);
    }
}
