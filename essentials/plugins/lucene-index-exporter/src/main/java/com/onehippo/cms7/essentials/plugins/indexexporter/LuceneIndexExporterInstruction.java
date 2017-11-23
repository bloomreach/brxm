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

package com.onehippo.cms7.essentials.plugins.indexexporter;

import com.google.common.collect.Multimap;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.utils.WebXmlUtils;
import org.onehippo.repository.jaxrs.RepositoryJaxrsServlet;

public class LuceneIndexExporterInstruction implements Instruction {
    private static final String SERVLET_NAME = "RepositoryJaxrsServlet";

    @Override
    public InstructionStatus execute(PluginContext context) {
        return WebXmlUtils.addServlet(context, TargetPom.CMS, SERVLET_NAME, RepositoryJaxrsServlet.class,
                6, new String[]{"/ws/*"}) ? InstructionStatus.SUCCESS : InstructionStatus.FAILED;
    }

    @Override
    public Multimap<MessageGroup, String> getChangeMessages() {
        return Instruction.makeChangeMessages(MessageGroup.EXECUTE,
                "Ensure availability of '" + SERVLET_NAME + "' through cms web.xml");
    }
}
