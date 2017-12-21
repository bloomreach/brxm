/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.inject.Inject;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class FreemarkerInstructionTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(FreemarkerInstructionTest.class);

    @Inject private FreemarkerInstruction instruction;

    @Test
    public void testProcess() throws Exception {
        instruction.setAction("copy");
        instruction.setSource("test_template_freemarker.ftl");
        instruction.setTarget("{{freemarkerRoot}}/{{namespace}}/homepage-main-content.ftl");
        log.info("instruction {}", instruction);
        assertTrue(instruction.valid());
    }
}