/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.instruction.parser;

import java.io.StringReader;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.onehippo.cms7.essentials.dashboard.instruction.PluginInstructions;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.utils.inject.EventBusModule;
import org.onehippo.cms7.essentials.dashboard.utils.inject.PropertiesModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @version "$Id$"
 */
public final class InstructionParser {

    private static Logger log = LoggerFactory.getLogger(InstructionParser.class);


    public static Instructions parseInstructions(final String content) {
        try {
            final JAXBContext context = JAXBContext.newInstance(PluginInstructions.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            final Instructions instructions = (Instructions) unmarshaller.unmarshal(new StringReader(content));
            final Injector injector = Guice.createInjector(new PropertiesModule(), EventBusModule.getInstance());
            final Set<InstructionSet> instructionSets = instructions.getInstructionSets();
            for (InstructionSet instructionSet : instructionSets) {
                final Set<Instruction> myInstr = instructionSet.getInstructions();
                for (Instruction instruction : myInstr) {
                    injector.injectMembers(instruction);
                }
            }

            injector.injectMembers(instructions);
            return instructions;

        } catch (JAXBException e) {
            log.error("Error parsing instruction", e);
        }

        return null;
    }

    private  InstructionParser() {
    }
}
