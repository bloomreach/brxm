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

package org.onehippo.cms7.essentials.dashboard.instruction;

import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.wicket.util.string.Strings;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.InstructionEvent;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "xml", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class XmlInstruction extends PluginInstruction {

    private static final Logger log = LoggerFactory.getLogger(XmlInstruction.class);
    private String message;
    private boolean override;
    private String source;
    private String target;
    private String action;

    @Inject
    private EventBus eventBus;

    @Inject(optional = true)
    @Named("instruction.message.xml.delete")
    private String messageDelete;

    @Inject(optional = true)
    @Named("instruction.message.xml.copy")
    private String messageCopy;

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        log.debug("XML Instruction");
        eventBus.post(new InstructionEvent(this));
        return InstructionStatus.FAILED;
    }


    @Override
    public void processPlaceholders(final Map<String, Object> data) {
        final String myTarget = TemplateUtils.replaceTemplateData(target, data);
        if (myTarget != null) {
            target = myTarget;
        }
        //
        final String mySource = TemplateUtils.replaceTemplateData(source, data);
        if (mySource != null) {
            source = mySource;
        }
        // add local data
        data.put(EssentialConst.PLACEHOLDER_SOURCE, source);
        data.put(EssentialConst.PLACEHOLDER_TARGET, target);
        // setup messages:

        if (Strings.isEmpty(message)) {
            // check message based on action:
            if (action.equals(COPY)) {
                message = messageCopy;
            } else if (action.equals(DELETE)) {
                message = messageDelete;
            }
        }

        super.processPlaceholders(data);
        //
        message = TemplateUtils.replaceTemplateData(message, data);
    }



    @XmlAttribute
    public boolean isOverride() {
        return override;
    }

    public void setOverride(final boolean override) {
        this.override = override;
    }

    @XmlAttribute
    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    @XmlAttribute
    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    @XmlAttribute
    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(final String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("XmlInstruction{");
        sb.append("message='").append(message).append('\'');
        sb.append(", override=").append(override);
        sb.append(", source='").append(source).append('\'');
        sb.append(", target='").append(target).append('\'');
        sb.append(", action='").append(action).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @XmlAttribute
    @Override
    public String getAction() {
        return action;
    }

    @Override
    public void setAction(final String action) {
        this.action = action;
    }

}
