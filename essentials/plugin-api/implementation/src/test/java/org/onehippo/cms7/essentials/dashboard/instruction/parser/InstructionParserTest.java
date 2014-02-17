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

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.instruction.FileInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.PluginInstructionSet;
import org.onehippo.cms7.essentials.dashboard.instruction.PluginInstructions;
import org.onehippo.cms7.essentials.dashboard.instruction.XmlInstruction;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */

public class InstructionParserTest extends BaseTest {

    private static Logger log = LoggerFactory.getLogger(InstructionParserTest.class);


    @Inject
    private InstructionParser instructionParser;


    @Test
    public void testParseInstructions() throws Exception {


        //############################################
        // READ FROM FILE
        //############################################
        final InputStream resourceAsStream = getClass().getResourceAsStream("/instructions.xml");
        final String content = GlobalUtils.readStreamAsText(resourceAsStream);
        final Instructions myInstructions = instructionParser.parseInstructions(content);
        final Set<InstructionSet> iset = myInstructions.getInstructionSets();
        assertEquals(2, iset.size());

        final Iterator<InstructionSet> myIterator = iset.iterator();
        final InstructionSet set1 = myIterator.next();
        final InstructionSet set2 = myIterator.next();

        assertEquals("myGroup", set1.getGroup());
        assertEquals(EssentialConst.INSTRUCTION_GROUP_DEFAULT, set2.getGroup());
        assertEquals(5, set1.getInstructions().size());
        assertEquals(1, set2.getInstructions().size());
        // total instructions is 6:
        assertEquals(6, myInstructions.totalInstructions());
        assertEquals(2, myInstructions.totalInstructionSets());



        //############################################
        // OBJECTS
        //############################################

        final Instructions value = new PluginInstructions();
        final Set<InstructionSet> instructionSets = new HashSet<>();
        final PluginInstructionSet instructionSet = new PluginInstructionSet();
        addInstructions(instructionSet);

        instructionSets.add(instructionSet);
        value.setInstructionSets(instructionSets);
        final JAXBContext context = JAXBContext.newInstance(PluginInstructions.class);
        final Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final StringWriter writer = new StringWriter();
        m.marshal(value, writer);
        final String s = writer.toString();
        log.info("s {}", s);
        final Instructions instructions = instructionParser.parseInstructions(s);
        assertTrue(instructions != null);
        final InstructionSet set = instructions.getInstructionSets().iterator().next();
        final Set<Instruction> mySet = set.getInstructions();
        // test ordering
        final Iterator<Instruction> iterator = mySet.iterator();
        for (int i = 0; i < mySet.size(); i++) {
            final Instruction next = iterator.next();
            final int isMod3 = i % 3;
            if (isMod3 == 0) {
                assertTrue(next.getMessage().equals(String.format("XML%d", i)));
            } else {
                assertTrue(next.getMessage().equals(String.format("FILE%d", i)));
            }
        }
    }

    private void addInstructions(final PluginInstructionSet pluginInstructionSet) {
        for (int i = 0; i < 100; i++) {
            final int isMod3 = i % 3;
            final Instruction instruction = isMod3 == 0 ? new XmlInstruction() : new FileInstruction();
            if (isMod3 == 0) {
                instruction.setMessage(String.format("XML%d", i));
            } else {
                instruction.setMessage(String.format("FILE%d", i));
            }
            pluginInstructionSet.addInstruction(instruction);
        }
    }

    @Test
    public void initTest() {
        assertTrue(true);
    }
}
