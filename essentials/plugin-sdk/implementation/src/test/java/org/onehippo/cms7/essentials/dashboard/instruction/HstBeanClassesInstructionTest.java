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

    @Test
    public void default_change_message() {
        final HstBeanClassesInstruction instruction = new HstBeanClassesInstruction();
        instruction.setPattern("foo");
        final Multimap<MessageGroup, String> changeMessages = instruction.getChangeMessages();
        assertEquals(1, changeMessages.keySet().size());
        assertTrue(changeMessages.containsKey(MessageGroup.EXECUTE));
        assertEquals(1, changeMessages.get(MessageGroup.EXECUTE).size());
        assertEquals("Add mapping 'foo' for annotated HST beans to Site web.xml.",
                changeMessages.get(MessageGroup.EXECUTE).iterator().next());
        assertEquals("foo", instruction.getPattern());
    }

    @Test
    public void xml_based_change_message() {
        final String message = "anything";
        final HstBeanClassesInstruction instruction = new HstBeanClassesInstruction();
        instruction.setMessage(message);
        final Multimap<MessageGroup, String> changeMessages = instruction.getChangeMessages();
        assertTrue(changeMessages.containsKey(MessageGroup.EXECUTE));
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
