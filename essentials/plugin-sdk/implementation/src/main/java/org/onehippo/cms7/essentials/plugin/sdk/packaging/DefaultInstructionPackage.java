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

package org.onehippo.cms7.essentials.plugin.sdk.packaging;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructionSet;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructions;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.parser.DefaultInstructionParser;
import org.onehippo.cms7.essentials.plugin.sdk.rest.ChangeMessage;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.TemplateUtils;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.service.PlaceholderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of a instruction package. Reads instruction XML from {@code /META-INF/instructions.xml}
 *
 * @version "$Id$"
 */
public class DefaultInstructionPackage {

    private static Logger log = LoggerFactory.getLogger(DefaultInstructionPackage.class);

    private final PluginInstructionExecutor executor;
    private final DefaultInstructionParser instructionParser;
    private final PlaceholderService placeholderService;

    private PluginInstructions instructions;
    private String path;

    @Inject
    public DefaultInstructionPackage(final PluginInstructionExecutor executor,
                                     final DefaultInstructionParser instructionParser,
                                     final PlaceholderService placeholderService) {
        this.executor = executor;
        this.instructionParser = instructionParser;
        this.placeholderService = placeholderService;
    }

    public List<ChangeMessage> getInstructionsMessages(final Map<String, Object> parameters) {
        final PluginInstructions myInstructions = getInstructions();
        if (myInstructions == null) {
            return Collections.emptyList();
        }
        final Multimap<Instruction.Type, String> messageMap = ArrayListMultimap.create();
        for (PluginInstructionSet instructionSet : instructions.getInstructionSets()) {
            if (shouldExecuteInstructionSet(instructionSet, parameters)) {
                executor.getInstructionsMessages(instructionSet, messageMap);
            }
        }

        final Map<String, Object> placeholderData = placeholderService.makePlaceholders();
        return messageMap.entries().stream()
                .map(entry -> new ChangeMessage(TemplateUtils.replaceTemplateData(entry.getValue(), placeholderData), entry.getKey()))
                .collect(Collectors.toList());
    }


    public String getInstructionPath() {
        if (Strings.isNullOrEmpty(path)) {
            return EssentialConst.DEFAULT_INSTRUCTIONS_PATH;
        }
        return path;
    }

    public void setInstructionPath(final String path) {
        this.path = path;
    }

    public PluginInstructions getInstructions() {
        if (instructions == null) {
            final InputStream resourceAsStream = getClass().getResourceAsStream(getInstructionPath());
            final String content = GlobalUtils.readStreamAsText(resourceAsStream);
            instructions = instructionParser.parseInstructions(content);
        }
        return instructions;
    }

    public Instruction.Status execute(final Map<String, Object> parameters) {
        if (instructions == null) {
            instructions = getInstructions();
        }
        if (instructions == null) {
            log.error("Failed to parse instructions at '{}'.", getInstructionPath());
            return Instruction.Status.FAILED;
        }
        final Set<PluginInstructionSet> instructionSets = instructions.getInstructionSets();
        Instruction.Status status = Instruction.Status.SUCCESS;
        for (PluginInstructionSet instructionSet : instructionSets) {
            if (shouldExecuteInstructionSet(instructionSet, parameters)) {
                final Instruction.Status sts = executor.execute(instructionSet, parameters);
                if (sts == Instruction.Status.FAILED) {
                    status = sts; // remember the 'worst' result
                }
            }
        }
        return status;
    }

    private boolean shouldExecuteInstructionSet(final PluginInstructionSet instructionSet, final Map<String, Object> parameters) {
        for (String group : instructionSet.getGroups()) {
            // always execute default instruction sets
            if (EssentialConst.INSTRUCTION_GROUP_DEFAULT.equals(group)) {
                return true;
            }

            final Object value = parameters.get(group);
            if (value instanceof Boolean && (Boolean)value) {
                return true;
            }
        }
        return false;
    }
}
