/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@XmlRootElement(name = "translations", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class TranslationsInstruction extends BuiltinInstruction {

    private static final Logger LOG = LoggerFactory.getLogger(TranslationsInstruction.class);

    @Inject private JcrService jcrService;

    private String source;

    public TranslationsInstruction() {
        super(MessageGroup.XML_NODE_CREATE);
    }

    @XmlAttribute
    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    @Override
    public InstructionStatus execute(final PluginContext context) {
        final Session session = jcrService.createSession();
        boolean success = false;
        try {
            success = jcrService.importTranslationsResource(session, source, context.getPlaceholderData());
            session.save();
        } catch (RepositoryException e) {
            LOG.error("Failed to import translations into repository.", e);
        } finally {
            jcrService.destroySession(session);
        }
        return success ? InstructionStatus.SUCCESS : InstructionStatus.FAILED;
    }

    @Override
    void populateDefaultChangeMessages(final BiConsumer<MessageGroup, String> changeMessageQueue) {
        changeMessageQueue.accept(getDefaultGroup(), "Import repository translations from '" + source + "'.");
    }
}
