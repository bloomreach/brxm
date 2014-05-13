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

package org.onehippo.cms7.essentials.dashboard.packaging;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionParser;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.model.Restful;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;

/**
 * Default implementation of a instruction package. Reads instruction XML from {@code /META-INF/instructions.xml}
 *
 * @version "$Id$"
 */
public class DefaultInstructionPackage implements InstructionPackage {


    public static final ImmutableSet<String> DEFAULT_GROUPS = new ImmutableSet.Builder<String>().add(EssentialConst.INSTRUCTION_GROUP_DEFAULT).build();
    public static final String DEFAULT_INSTRUCTIONS_PATH = "/META-INF/instructions.xml";
    public static final String PROP_TEMPLATE_NAME = "templateName";
    public static final String PROP_SAMPLE_DATA = "sampleData";
    private static Logger log = LoggerFactory.getLogger(DefaultInstructionPackage.class);

    @Inject
    private InstructionParser instructionParser;

    @Inject
    private EventBus eventBus;

    private Map<String, Object> properties;
    private Instructions instructions;
    private String path;

    @Override
    public Map<String, Object> getProperties() {

        if (properties == null) {
            return new HashMap<>();
        }
        return properties;
    }

    @Override
    public void setProperties(final Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public Set<String> groupNames() {
        return DEFAULT_GROUPS;
    }

    @Override
    public Set<? extends Restful> getInstructionsMessages(final PluginContext context) {
        final Instructions myInstructions = getInstructions();
        if (myInstructions == null) {
            return Collections.emptySet();

        }
        final Set<InstructionSet> instructionSets = instructions.getInstructionSets();
        final InstructionExecutor executor = new PluginInstructionExecutor();
        final Set<String> myGroupNames = groupNames();
        final Set<KeyValueRestful> instructionsMessages = new HashSet<>();
        for (InstructionSet instructionSet : instructionSets) {
            final String group = instructionSet.getGroup();
            // execute only or group(s)
            if (myGroupNames.contains(group)) {
                final Set<KeyValueRestful> instructionsMessages1 = executor.getInstructionsMessages(instructionSet, context);
                instructionsMessages.addAll(instructionsMessages1);

            } else {
                log.debug("Skipping instruction group for name: [{}]", group);
            }
        }
        return instructionsMessages;
    }


    @Override
    public String getInstructionPath() {
        if (Strings.isNullOrEmpty(path)) {
            return DEFAULT_INSTRUCTIONS_PATH;
        }
        return path;
    }


    @Override
    public void setInstructionPath(final String path) {
        this.path = path;
    }

    @Override
    public Instructions getInstructions() {
        if (instructions == null) {

            final InputStream resourceAsStream = getClass().getResourceAsStream(getInstructionPath());
            final String content = GlobalUtils.readStreamAsText(resourceAsStream);
            if (instructionParser == null) {
                final StringBuilder errorBuilder = new StringBuilder();
                errorBuilder.append('\n')
                        .append("=============================================================================")
                        .append('\n')
                        .append("InstructionParser was null. This can means it is not injected by Spring.")
                        .append('\n')
                        .append("If you are running a test case, make sure you autowire beans like:")
                        .append('\n')
                        .append("InstructionPackage instructionPackage = new MyInstructionPackage();injector.autowireBean(instructionPackage);")
                        .append('\n')
                        .append("=============================================================================")
                        .append('\n');

                log.error("{}", errorBuilder);
                throw new IllegalArgumentException("Instruction parser not injected");
            }
            instructions = instructionParser.parseInstructions(content);
        }
        return instructions;

    }

    @Override
    public InstructionStatus execute(final PluginContext context) {
        // NOTE: we'll add any additional context properties into context:
        context.addPlaceholderData(properties);


        if (instructions == null) {
            instructions = getInstructions();
        }
        if (instructions == null) {
            eventBus.post(new DisplayEvent("Couldn't parse instructions"));
            log.error("Failed to parse instructions");
            return InstructionStatus.FAILED;
        }
        final Set<InstructionSet> instructionSets = instructions.getInstructionSets();
        InstructionStatus status = InstructionStatus.SUCCESS;
        final InstructionExecutor executor = new PluginInstructionExecutor();
        final Set<String> myGroupNames = groupNames();
        for (InstructionSet instructionSet : instructionSets) {
            final String group = instructionSet.getGroup();
            // execute only or group(s)
            if (myGroupNames.contains(group)) {
                // currently we return fail if any of instructions is failed
                if (status == InstructionStatus.FAILED) {
                    executor.execute(instructionSet, context);
                    continue;
                }
                status = executor.execute(instructionSet, context);
            } else {
                log.debug("Skipping instruction group for name: [{}]", group);
            }
        }
        return status;
    }

    @Override
    public InstructionParser getInstructionParser() {
        return instructionParser;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
