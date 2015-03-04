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

package org.onehippo.cms7.essentials.dashboard.instruction;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.FileUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@Component
@XmlRootElement(name = "directory", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class DirectoryInstruction extends PluginInstruction {

    private static final Logger log = LoggerFactory.getLogger(DirectoryInstruction.class);
    private PluginContext context;
    private String target;
    private String action;
    private String message;

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        if (Strings.isNullOrEmpty(action)) {
            log.warn("DirectoryInstruction: action was empty");
            message = "Failed to process instruction: invalid action";
            return InstructionStatus.FAILED;
        }
        if (Strings.isNullOrEmpty(target)) {
            log.warn("DirectoryInstruction: target was empty");
            message = "Failed to create directory: invalid name";
            return InstructionStatus.FAILED;
        }
        if (!action.equals("create")) {
            message = "Failed to process instruction: invalid action";
            throw new IllegalStateException("Not implemented yet: " + action);
        }
        processPlaceholders(context.getPlaceholderData());
        log.debug("Creating directory: {}", target);
        try {
            FileUtils.createParentDirectories(new File(target));
        } catch (IOException e) {
            log.error("Error creating directory: " + target, e);
            message = "Failed to create directory " + target;
            return InstructionStatus.FAILED;
        }
        this.context = context;
        message = "Created directory " + target;
        return InstructionStatus.SUCCESS;

    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) {
        super.processPlaceholders(data);
        final String myTarget = TemplateUtils.replaceTemplateData(target, data);
        if (myTarget != null) {
            target = myTarget;
        }
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(final String message) {

    }

    @XmlAttribute
    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = target;
    }


    @Override
    public String getAction() {
        return action;
    }

    @XmlAttribute
    @Override
    public void setAction(final String action) {
        this.action = action;
    }


}
