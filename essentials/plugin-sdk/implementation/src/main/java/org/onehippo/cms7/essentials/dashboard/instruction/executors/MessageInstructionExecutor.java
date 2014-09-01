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
import java.util.Map;
import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instruction.CndInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.ExecuteInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.FileInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.FreemarkerInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.NodeFolderInstruction;
import org.onehippo.cms7.essentials.dashboard.instruction.XmlInstruction;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.model.Restful;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Unlike {@code PluginInstructionExecutor}, {@code MessageInstructionExecutor} does not execute any instructions,
 * but returns set of messages. Those can be used and presented to user before we execute anything.
 *
 * @version "$Id$"
 * @see org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor
 */
public class MessageInstructionExecutor {


    @SuppressWarnings("InstanceofInterfaces")
    public Multimap<MessageGroup, Restful> execute(final InstructionSet instructionSet, PluginContext context) {


        final Multimap<MessageGroup, Restful> retVal = ArrayListMultimap.create();
        final Map<String, Object> placeholderData = context.getPlaceholderData();

        final Set<Instruction> mySet = instructionSet.getInstructions();
        for (Instruction instruction : mySet) {
            if (instruction instanceof FreemarkerInstruction) {
                processFreemarkerInstruction(retVal, placeholderData, (FreemarkerInstruction) instruction);
            } else if (instruction instanceof FileInstruction) {
                processFileInstruction(retVal, placeholderData, (FileInstruction) instruction);

            } else if (instruction instanceof NodeFolderInstruction) {
                processFolderInstruction(retVal, placeholderData, (NodeFolderInstruction) instruction);
            } else if (instruction instanceof CndInstruction) {
                processCndInstruction(retVal, placeholderData, (CndInstruction) instruction);
            } else if (instruction instanceof XmlInstruction) {
                processXmlInstruction(retVal, placeholderData, (XmlInstruction) instruction);
            }   else if (instruction instanceof ExecuteInstruction) {
                processXmlInstruction(retVal, placeholderData, (ExecuteInstruction) instruction);
            } else {
                retVal.put(MessageGroup.UNKNOWN, new MessageRestful(instruction.getMessage()));
            }
        }

        return retVal;
    }


    private void processXmlInstruction(final Multimap<MessageGroup, Restful> retVal, final Map<String, Object> placeholderData, final ExecuteInstruction instruction) {
        final Instruction executeInstruction = GlobalUtils.newInstance(instruction.getClazz());
        retVal.put(MessageGroup.EXECUTE, new MessageRestful(executeInstruction.getMessage()));
    }


    private void processXmlInstruction(final Multimap<MessageGroup, Restful> retVal, final Map<String, Object> placeholderData, final XmlInstruction instruction) {
        final String replacedTarget = TemplateUtils.replaceTemplateData(instruction.getTarget(), placeholderData);
        if (instruction.getAction().equals("copy")) {
            final String replacedSource = TemplateUtils.replaceTemplateData(instruction.getSource(), placeholderData);
            final String userMessage = MessageFormat.format("File: {0}  will be imported into following location: {1}", replacedSource, replacedTarget);
            Restful keyValueRestful = new MessageRestful(userMessage);
            retVal.put(MessageGroup.XML_NODE_CREATE, keyValueRestful);
        } else {
            final String userMessage = MessageFormat.format("Following node will be deleted: {0}", replacedTarget);
            Restful keyValueRestful = new MessageRestful(userMessage);
            retVal.put(MessageGroup.XML_NODE_CREATE, keyValueRestful);
        }
    }

    private void processCndInstruction(final Multimap<MessageGroup, Restful> retVal, final Map<String, Object> placeholderData, final CndInstruction instr) {
        final String documentType = instr.getDocumentType();
        final String replacedData = TemplateUtils.replaceTemplateData(documentType, placeholderData);
        final String userMessage = MessageFormat.format("New document type will be registered:  {0}", replacedData);
        Restful keyValueRestful = new MessageRestful(userMessage);
        retVal.put(MessageGroup.DOCUMENT_REGISTER, keyValueRestful);
    }


    private void processFolderInstruction(final Multimap<MessageGroup, Restful> retVal, final Map<String, Object> placeholderData, final NodeFolderInstruction instruction) {
        final String path = instruction.getPath();
        final String replacedData = TemplateUtils.replaceTemplateData(path, placeholderData);
        final String userMessage = MessageFormat.format("Following repository folder will be created: {0}", replacedData);
        Restful keyValueRestful = new MessageRestful(userMessage);
        retVal.put(MessageGroup.XML_NODE_FOLDER_CREATE, keyValueRestful);
    }

    private void processFileInstruction(final Multimap<MessageGroup, Restful> retVal, final Map<String, Object> placeholderData, final FileInstruction instruction) {
        final String action = instruction.getAction();
        if (action.equals("copy")) {

            final String replacedData = TemplateUtils.replaceTemplateData(instruction.getTarget(), placeholderData);
            final String userMessage = MessageFormat.format("New file will be created: {0}", replacedData);
            Restful keyValueRestful = new MessageRestful(userMessage);
            retVal.put(MessageGroup.FILE_CREATE, keyValueRestful);
        } else if (action.equals("delete")) {
            final String replacedData = TemplateUtils.replaceTemplateData(instruction.getTarget(), placeholderData);
            final String userMessage = MessageFormat.format("Following file will be deleted: {0}", replacedData);
            Restful keyValueRestful = new MessageRestful(userMessage);
            retVal.put(MessageGroup.FILE_DELETE, keyValueRestful);
        }
    }

    private void processFreemarkerInstruction(final Multimap<MessageGroup, Restful> retVal, final Map<String, Object> placeholderData, final FreemarkerInstruction instruction) {
        final String action = instruction.getAction();
        // process placeholder
        instruction.processPlaceholders(placeholderData);
        if (action.equals("copy")) {
            final String replacedData = TemplateUtils.replaceTemplateData(instruction.getTarget(), placeholderData);
            final String userMessage;
            if (instruction.isRepoBased()) {
                userMessage = MessageFormat.format("New HST template node will be created: {0}", instruction.getTarget());
                Restful keyValueRestful = new MessageRestful(userMessage);
                retVal.put(MessageGroup.XML_NODE_CREATE, keyValueRestful);
            } else {
                userMessage = MessageFormat.format("New file will be created: {0}", replacedData);
                Restful keyValueRestful = new MessageRestful(userMessage);
                retVal.put(MessageGroup.FILE_CREATE, keyValueRestful);
            }



        } else if (action.equals("delete")) {
            final String replacedData = TemplateUtils.replaceTemplateData(instruction.getTarget(), placeholderData);
            final String userMessage = MessageFormat.format("Following file will be deleted: {0}", replacedData);
            Restful keyValueRestful = new MessageRestful(userMessage);
            retVal.put(MessageGroup.FILE_DELETE, keyValueRestful);
        }
    }

}
