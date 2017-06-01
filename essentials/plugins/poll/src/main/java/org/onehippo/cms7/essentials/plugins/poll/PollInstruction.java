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

package org.onehippo.cms7.essentials.plugins.poll;

import java.io.FileNotFoundException;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.WebXmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add a bean mapping to the Site's web.xml file.
 */
public class PollInstruction implements Instruction {

    private static Logger log = LoggerFactory.getLogger(PollInstruction.class);
    private static final String BEANS_MAPPING = "classpath*:org/onehippo/forge/**/*.class";

    @Override
    public String getMessage() {
        return "Adding bean mapping to web.xml: " + BEANS_MAPPING;
    }

    @Override
    public void setMessage(final String message) {

    }

    @Override
    public String getAction() {
        return null;
    }

    @Override
    public void setAction(final String action) {

    }

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        try {
            if (WebXmlUtils.addHstBeanMapping(context, BEANS_MAPPING)) {
                return InstructionStatus.SUCCESS;
            } else {
                return InstructionStatus.SKIPPED;
            }
        } catch (FileNotFoundException | JAXBException e) {
            log.error("Error executing robots plugin instruction", e);
        }
        return InstructionStatus.FAILED;
    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) {
        // noop
    }
}
