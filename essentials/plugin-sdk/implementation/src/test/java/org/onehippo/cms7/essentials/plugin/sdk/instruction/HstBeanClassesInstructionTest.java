/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.plugin.sdk.services.WebXmlServiceImpl;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.service.WebXmlService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HstBeanClassesInstructionTest extends BaseTest {

    private static Map<String, Object> dummyParameters = new HashMap<>();

    @Inject private ProjectService projectService;

    private String relayedPattern;
    private boolean result;
    private int counter;

    @Test
    public void default_change_message() {
        final HstBeanClassesInstruction instruction = new HstBeanClassesInstruction();
        final BiConsumer<Instruction.Type, String> collector = (g, m) -> {
            assertTrue(g == Instruction.Type.EXECUTE);
            assertEquals("Add mapping 'foo' for annotated HST beans to Site web.xml.", m);
            counter++;
        };
        instruction.setPattern("foo");
        assertEquals("foo", instruction.getPattern());

        counter = 0;
        instruction.populateChangeMessages(collector);
        assertEquals(1, counter);
    }

    @Test
    public void xml_based_change_message() {
        final String message = "anything";
        final HstBeanClassesInstruction instruction = new HstBeanClassesInstruction();
        final BiConsumer<Instruction.Type, String> collector = (g, m) -> {
            assertTrue(g == Instruction.Type.EXECUTE);
            assertEquals(message, m);
            counter++;
        };
        instruction.setMessage(message);
        assertEquals(message, instruction.getMessage());

        counter = 0;
        instruction.populateChangeMessages(collector);
        assertEquals(1, counter);
    }

    @Test
    public void relay_to_service() {
        final WebXmlService webXmlService = new WebXmlServiceImpl(projectService) {
            public boolean addHstBeanClassPattern(final String pattern) {
                relayedPattern = pattern;
                return result;
            }
        };
        final HstBeanClassesInstruction instruction = new HstBeanClassesInstruction();
        instruction.setPattern("foo");
        instruction.webXmlService = webXmlService;

        result = true;
        assertEquals(Instruction.Status.SUCCESS, instruction.execute(dummyParameters));
        assertEquals("foo", relayedPattern);

        result = false;
        assertEquals(Instruction.Status.FAILED, instruction.execute(dummyParameters));
    }
}
