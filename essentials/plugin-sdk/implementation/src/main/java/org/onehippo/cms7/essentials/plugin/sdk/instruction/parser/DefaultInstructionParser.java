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

package org.onehippo.cms7.essentials.plugin.sdk.instruction.parser;

import java.io.StringReader;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.google.common.base.Strings;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructionSet;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;


/**
 * @version "$Id$"
 */
@Component
public class DefaultInstructionParser {

    private static Logger log = LoggerFactory.getLogger(DefaultInstructionParser.class);

    @Inject
    private AutowireCapableBeanFactory injector;

    public PluginInstructions parseInstructions(final String content) {
        if (Strings.isNullOrEmpty(content)) {
            log.warn("Instructions content was null");
            return null;
        }
        try {
            final JAXBContext context = JAXBContext.newInstance(PluginInstructions.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            final PluginInstructions instructions = (PluginInstructions) unmarshaller.unmarshal(new StringReader(content));

            // Inject instructions
            for (PluginInstructionSet instructionSet : instructions.getInstructionSets()) {
                for (Instruction instruction : instructionSet.getInstructions()) {
                    injector.autowireBean(instruction);
                }
            }

            return instructions;
        } catch (JAXBException e) {
            log.error("Error parsing instruction", e);
        }

        return null;
    }
}
