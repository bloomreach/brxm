/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.instruction;

import java.util.function.BiConsumer;

import com.google.common.collect.Multimap;

import org.junit.Test;
import org.onehippo.cms7.essentials.MockPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.service.WebXmlService;
import org.onehippo.cms7.essentials.dashboard.services.WebXmlServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HstBeanClassesInstructionTest {
    private PluginContext relayedContext;
    private String relayedPattern;
    private boolean result;
    private int counter;

    @Test
    public void default_change_message() {
        final HstBeanClassesInstruction instruction = new HstBeanClassesInstruction();
        final BiConsumer<MessageGroup, String> collector = (g, m) -> {
            assertTrue(g == MessageGroup.EXECUTE);
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
        final BiConsumer<MessageGroup, String> collector = (g, m) -> {
            assertTrue(g == MessageGroup.EXECUTE);
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
        final WebXmlService webXmlService = new WebXmlServiceImpl() {
            public boolean addHstBeanClassPattern(final PluginContext context, final String pattern) {
                relayedContext = context;
                relayedPattern = pattern;
                return result;
            }
        };
        final PluginContext context = new MockPluginContext();
        final HstBeanClassesInstruction instruction = new HstBeanClassesInstruction();
        instruction.setPattern("foo");
        instruction.webXmlService = webXmlService;

        result = true;
        assertEquals(InstructionStatus.SUCCESS, instruction.execute(context));
        assertEquals("foo", relayedPattern);
        assertTrue(context == relayedContext);

        result = false;
        assertEquals(InstructionStatus.FAILED, instruction.execute(context));
    }
}
