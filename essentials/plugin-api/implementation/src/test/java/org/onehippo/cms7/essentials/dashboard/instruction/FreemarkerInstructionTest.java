/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FreemarkerInstructionTest extends BaseRepositoryTest {

    public static final String SOURCE = "test_template_freemarker.ftl";
    private static final Logger log = LoggerFactory.getLogger(FreemarkerInstructionTest.class);
    @Inject
    private InstructionExecutor executor;
    @Inject

    private FreemarkerInstruction instruction;

    @Test
    public void testProcess() throws Exception {
        super.createHstRootConfig();
        instruction.setAction("copy");
        instruction.setSource(SOURCE);
        instruction.setTarget("{{freemarkerRoot}}/{{namespace}}/homepage-main-content.ftl");
        log.info("instruction {}", instruction);
        assertTrue(instruction.valid());
        final PluginContext context = getContext();
        context.addPlaceholderData(EssentialConst.PROP_TEMPLATE_NAME, EssentialConst.TEMPLATE_FREEMARKER);
        context.addPlaceholderData(EssentialConst.TEMPLATE_PARAM_FILE_BASED, false);
        context.addPlaceholderData(EssentialConst.TEMPLATE_PARAM_REPOSITORY_BASED, true);
        assertTrue(instruction.valid());
        final String projectNamespace = (String) context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_NAMESPACE);
        log.info("projectNamespace {}", projectNamespace);
        instruction.process(context, InstructionStatus.SUCCESS);
        assertEquals("/hst:hst/hst:configurations/" + projectNamespace + "/hst:templates", instruction.getRepositoryTarget());
        assertEquals("homepage-main-content.ftl", instruction.getTemplateName());
        instruction.process(context, InstructionStatus.SKIPPED);


    }


}