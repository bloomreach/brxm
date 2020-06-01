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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.Session;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TranslationsInstructionTest extends BaseRepositoryTest {

    private static Map<String, Object> dummyParameters = new HashMap<>();

    @Inject private AutowireCapableBeanFactory injector;

    @Test
    public void testInstruction() throws Exception {
        final TranslationsInstruction instruction = new TranslationsInstruction();
        injector.autowireBean(instruction);

        instruction.setSource("/instruction_translations_file.json");
        assertEquals(Instruction.Status.SUCCESS,instruction.execute(dummyParameters));

        final Session session = jcrService.createSession();
        assertTrue(session.itemExists("/hippo:configuration/hippo:translations/foo/bar/nl"));
        jcrService.destroySession(session);
    }
}
