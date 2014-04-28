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

package org.onehippo.cms7.essentials.dashboard.instruction.executors;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instruction.CndInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.FileInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.NodeFolderInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.XmlInstruction;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unlike {@code PluginInstructionExecutor}, {@code MessageInstructionExecutor} does not execute any instructions,
 * but returns set of messages. Those can be used and presented to user before we execute anything.
 *
 * @version "$Id$"
 * @see org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor
 */
public class MessageInstructionExecutor {

    private static Logger log = LoggerFactory.getLogger(MessageInstructionExecutor.class);


    public Set<KeyValueRestful> execute(final InstructionSet instructionSet, PluginContext context) {


        final Set<KeyValueRestful> retVal = new HashSet<>();
        final Map<String, Object> placeholderData = context.getPlaceholderData();

        final Set<Instruction> mySet = instructionSet.getInstructions();
        for (Instruction instruction : mySet) {
            if (instruction instanceof FileInstruction) {
                processFileInstruction(retVal, placeholderData, (FileInstruction) instruction);

            } else if (instruction instanceof NodeFolderInstruction) {
                processFolderInstruction(retVal, placeholderData, (NodeFolderInstruction) instruction);
            } else if (instruction instanceof CndInstruction) {
                processCndInstruction(retVal, placeholderData, (CndInstruction) instruction);
            } else if (instruction instanceof XmlInstruction) {
                processXmlInstruction(retVal, placeholderData, (XmlInstruction) instruction);
            } else {
                retVal.add(new KeyValueRestful(instruction.getAction(), instruction.getMessage()));
            }
        }

        return retVal;
    }

    private void processXmlInstruction(final Collection<KeyValueRestful> retVal, final Map<String, Object> placeholderData, final XmlInstruction instruction) {
        final String replacedTarget = TemplateUtils.replaceTemplateData(instruction.getTarget(), placeholderData);
        if (instruction.getAction().equals("copy")) {
            final String replacedSource = TemplateUtils.replaceTemplateData(instruction.getSource(), placeholderData);
            final String userMessage = MessageFormat.format("XML file: {0}  will be imported into following location: {1}", replacedSource, replacedTarget);
            KeyValueRestful keyValueRestful = new KeyValueRestful("XmlInstruction", userMessage);
            retVal.add(keyValueRestful);
        } else {
            final String userMessage = MessageFormat.format("Following node will be deleted: {0}", replacedTarget);
            KeyValueRestful keyValueRestful = new KeyValueRestful("XmlInstruction", userMessage);
            retVal.add(keyValueRestful);
        }
    }

    private void processCndInstruction(final Collection<KeyValueRestful> retVal, final Map<String, Object> placeholderData, final CndInstruction instr) {
        final String documentType = instr.getDocumentType();
        final String replacedData = TemplateUtils.replaceTemplateData(documentType, placeholderData);
        final String userMessage = MessageFormat.format("New document type will be registered:  {0}", replacedData);
        KeyValueRestful keyValueRestful = new KeyValueRestful("CndInstruction", userMessage);
        retVal.add(keyValueRestful);
    }

    private void processFolderInstruction(final Collection<KeyValueRestful> retVal, final Map<String, Object> placeholderData, final NodeFolderInstruction instruction) {
        final String path = instruction.getPath();
        final String replacedData = TemplateUtils.replaceTemplateData(path, placeholderData);
        final String userMessage = MessageFormat.format("Following repository folder will be created: {0}", replacedData);
        KeyValueRestful keyValueRestful = new KeyValueRestful("NodeFolderInstruction", userMessage);
        retVal.add(keyValueRestful);
    }

    private void processFileInstruction(final Collection<KeyValueRestful> retVal, final Map<String, Object> placeholderData, final FileInstruction instruction) {
        log.info("instruction {}", instruction.getClass());
        final String action = instruction.getAction();
        if (action.equals("copy")) {

            final String replacedData = TemplateUtils.replaceTemplateData(instruction.getTarget(), placeholderData);
            final String userMessage = MessageFormat.format("New file will be created: {0}", replacedData);
            KeyValueRestful keyValueRestful = new KeyValueRestful("FileInstruction", userMessage);
            retVal.add(keyValueRestful);
        } else if (action.equals("delete")) {
            final String replacedData = TemplateUtils.replaceTemplateData(instruction.getTarget(), placeholderData);
            final String userMessage = MessageFormat.format("Following file will be deleted: {0}", replacedData);
            KeyValueRestful keyValueRestful = new KeyValueRestful("FileInstruction", userMessage);
            retVal.add(keyValueRestful);
        }
    }

}
