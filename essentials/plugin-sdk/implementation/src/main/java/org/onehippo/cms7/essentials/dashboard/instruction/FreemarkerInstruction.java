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

package org.onehippo.cms7.essentials.dashboard.instruction;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @version "$Id$"
 */
@Component
@XmlRootElement(name = "freemarker", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class FreemarkerInstruction extends FileInstruction {

    private static Logger log = LoggerFactory.getLogger(FreemarkerInstruction.class);
    public static final ImmutableSet<String> DEFAULT_HST_TEMPLATES = new ImmutableSet.Builder<String>()
            .add("hstdefault")
            .add("include")
            .add("essentials")
            .build();

    private String repositoryTarget;
    private String templateName;

    @Override
    public InstructionStatus execute(final PluginContext context) {
        log.debug("executing Freemarker Instruction {}", this);
        processPlaceholders(context.getPlaceholderData());
        if (!valid()) {
            log.info("Invalid instruction descriptor: {}", toString());
            return InstructionStatus.FAILED;
        }

        return super.execute(context);

    }

    public void setRepositoryTarget(final String repositoryTarget) {
        this.repositoryTarget = repositoryTarget;
    }



    public String getTemplateName() {
        return templateName;
    }

    public String getRepositoryTarget() {
        return repositoryTarget;
    }

    @Override
    protected boolean valid() {
        final FileInstruction.Action action = getActionEnum();
        if (action == null) {
            return false;
        }
        if (action == FileInstruction.Action.COPY && Strings.isNullOrEmpty(getSource())) {
            return false;
        }
        // check if we have valid
        if (Strings.isNullOrEmpty(getTarget())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FreemarkerInstruction{");
        sb.append(super.toString());
        sb.append('}');
        return sb.toString();
    }
}
