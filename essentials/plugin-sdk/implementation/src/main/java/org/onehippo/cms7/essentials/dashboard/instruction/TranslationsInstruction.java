/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TranslationsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus.FAILED;
import static org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus.SUCCESS;

@Component
@XmlRootElement(name = "translations", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class TranslationsInstruction extends BuiltinInstruction {

    private static final Logger log = LoggerFactory.getLogger(TranslationsInstruction.class);

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
        final Session session = context.createSession();
        try (final InputStream in = getClass().getClassLoader().getResourceAsStream(source)) {
            final String json = TemplateUtils.replaceTemplateData(GlobalUtils.readStreamAsText(in), context.getPlaceholderData());
            TranslationsUtils.importTranslations(json, session);
            return SUCCESS;
        } catch (IOException | RepositoryException e) {
            log.error("Failed to import translations", e);
            return FAILED;
        } finally {
            session.logout();
        }
    }

    @Override
    void populateDefaultChangeMessages(final BiConsumer<MessageGroup, String> changeMessageQueue) {
        changeMessageQueue.accept(getDefaultGroup(), "Import repository translations from '" + source + "'.");
    }
}
