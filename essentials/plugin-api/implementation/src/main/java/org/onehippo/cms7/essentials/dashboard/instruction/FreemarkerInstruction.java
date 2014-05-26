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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.InstructionEvent;
import org.onehippo.cms7.essentials.dashboard.event.MessageEvent;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

/**
 * @version "$Id$"
 */
@Component
@XmlRootElement(name = "freemarker", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class FreemarkerInstruction extends FileInstruction {
    private static Logger log = LoggerFactory.getLogger(FreemarkerInstruction.class);


    @Inject
    private EventBus eventBus;
    private String repositoryTarget;



    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        log.debug("executing Freemarker Instruction {}", this);
        processPlaceholders(context.getPlaceholderData());
        if (!valid()) {
            eventBus.post(new MessageEvent("Invalid instruction descriptor: " + toString()));
            eventBus.post(new InstructionEvent(this));
            return InstructionStatus.FAILED;
        }
        final String templateName = (String) context.getPlaceholderData().get("templateName");
        boolean repoBased = false;
        if(!Strings.isNullOrEmpty(templateName) && templateName.equals("repository")){
            repoBased = true;
        }
        // check if repository template type:
        if (repoBased && !Strings.isNullOrEmpty(repositoryTarget)) {
            log.debug("Using repository stored freemarker templates");
            final Session session = context.createSession();
            InputStream stream = null;
            try {
                stream = extractStream();
                if (stream == null) {
                    log.error("Stream was null for source: {}", getSource());
                    return InstructionStatus.FAILED;
                }

                if (session.nodeExists(repositoryTarget)) {
                    log.debug("Node already exists {}", repositoryTarget);
                    return InstructionStatus.SKIPPED;
                }
                final int lastPathIndex = repositoryTarget.lastIndexOf('/');
                final String root = repositoryTarget.substring(0, lastPathIndex);
                final Node node = session.getNode(root);
                final String myTemplateName = repositoryTarget.substring(lastPathIndex + 1, repositoryTarget.length());
                log.debug("Adding freemarker template: {}", myTemplateName);
                final Node templateNode = node.addNode(myTemplateName, "hst:template");
                final String content = GlobalUtils.readStreamAsText(stream);
                final String replacedData = TemplateUtils.replaceTemplateData(content, context.getPlaceholderData());
                templateNode.setProperty("hst:script", replacedData);
                return InstructionStatus.SUCCESS;

            } catch (RepositoryException e) {
                log.error("Error writing freemarker template", e);
            } catch (FileNotFoundException e) {
                log.error("Error extracting stream", e);
            } finally {
                IOUtils.closeQuietly(stream);
                GlobalUtils.cleanupSession(session);
            }

            return InstructionStatus.SUCCESS;
        }
        return super.process(context, previousStatus);
    }

    @Override
    protected boolean valid() {
        if (Strings.isNullOrEmpty(getAction()) || !VALID_ACTIONS.contains(getAction())) {
            return false;
        }
        if (getAction().equals(COPY) && (Strings.isNullOrEmpty(getSource()))) {
            return false;
        }
        // check if we have valid
        if (Strings.isNullOrEmpty(getTarget()) && Strings.isNullOrEmpty(repositoryTarget)) {
            return false;
        }
        return true;
    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) {
        super.processPlaceholders(data);
        final String myRepoTarget = TemplateUtils.replaceTemplateData(repositoryTarget, data);
        if (myRepoTarget != null) {
            repositoryTarget = myRepoTarget;
        }
    }

    public String getRepositoryTarget() {
        return repositoryTarget;
    }

    public void setRepositoryTarget(final String repositoryTarget) {
        this.repositoryTarget = repositoryTarget;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FreemarkerInstruction{");
        sb.append("").append(super.toString());
        sb.append(", repositoryTarget=").append(repositoryTarget);
        sb.append('}');
        return sb.toString();
    }
}
