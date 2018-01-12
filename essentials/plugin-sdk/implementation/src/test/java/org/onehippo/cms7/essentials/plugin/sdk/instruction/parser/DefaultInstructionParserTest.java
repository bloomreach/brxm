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

import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.junit.Assert;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.BuiltinInstruction;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.FileInstruction;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructionSet;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructions;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.XmlInstruction;
import org.onehippo.cms7.essentials.plugin.sdk.install.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */

public class DefaultInstructionParserTest extends BaseTest {

    private static Logger log = LoggerFactory.getLogger(DefaultInstructionParserTest.class);


    @Inject private DefaultInstructionParser instructionParser;


    @Test
    public void testParseInstructions() throws Exception {


        //############################################
        // READ FROM FILE
        //############################################
        final InputStream resourceAsStream = getClass().getResourceAsStream("/parser_instructions.xml");
        final String content = GlobalUtils.readStreamAsText(resourceAsStream);
        final PluginInstructions myInstructions = instructionParser.parseInstructions(content);
        final Set<PluginInstructionSet> iset = myInstructions.getInstructionSets();
        assertEquals(3, iset.size());

        final Iterator<PluginInstructionSet> myIterator = iset.iterator();
        final PluginInstructionSet set1 = myIterator.next();
        final PluginInstructionSet set2 = myIterator.next();

        assertEquals("myGroup", set1.getGroups().iterator().next());
        Assert.assertEquals(EssentialConst.INSTRUCTION_GROUP_DEFAULT, set2.getGroups().iterator().next());
        assertEquals(8, set1.getInstructions().size());
        assertEquals(1, set2.getInstructions().size());
        // total instructions is 6:
        assertEquals(10, myInstructions.totalInstructions());
        assertEquals(3, myInstructions.totalInstructionSets());


        //############################################
        // OBJECTS
        //############################################

        final PluginInstructions value = new PluginInstructions();
        final Set<PluginInstructionSet> instructionSets = new HashSet<>();
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
        final PluginInstructions instructions = instructionParser.parseInstructions(s);
        assertTrue(instructions != null);
        final PluginInstructionSet set = instructions.getInstructionSets().iterator().next();
        final Set<Instruction> mySet = set.getInstructions();
        // test ordering
        final Iterator<Instruction> iterator = mySet.iterator();
        for (int i = 0; i < mySet.size(); i++) {
            final BuiltinInstruction next = (BuiltinInstruction)iterator.next();
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
            final BuiltinInstruction instruction = isMod3 == 0 ? new XmlInstruction() : new FileInstruction();
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
