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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

/**
 * @version "$Id$"
 */
@Component
@XmlRootElement(name = "freemarker", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class FreemarkerInstruction extends FileInstruction {
    private static final Pattern EXTENSION_REPLACEMENT = Pattern.compile(".ftl");
    private static Logger log = LoggerFactory.getLogger(FreemarkerInstruction.class);


    @Inject
    private EventBus eventBus;
    private String repositoryTarget;
    private String templateName;
    private boolean repoBased = false;


    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        log.debug("executing Freemarker Instruction {}", this);
        processPlaceholders(context.getPlaceholderData());
        if (!valid()) {
            eventBus.post(new MessageEvent("Invalid instruction descriptor: " + toString()));
            eventBus.post(new InstructionEvent(this));
            return InstructionStatus.FAILED;
        }
        defineTarget(context);
        // check if repository template type:
        if (repoBased && !Strings.isNullOrEmpty(repositoryTarget) && !Strings.isNullOrEmpty(templateName)) {
            log.debug("Using repository stored freemarker templates");
            final Session session = context.createSession();
            InputStream stream = null;
            try {
                stream = extractStream();
                if (stream == null) {
                    log.error("Stream was null for source: {}", getSource());
                    return InstructionStatus.FAILED;
                }
                final String absPath = repositoryTarget + '/' + templateName;
                if (session.nodeExists(absPath)) {
                    log.debug("Node already exists {}", absPath);
                    return InstructionStatus.SKIPPED;
                }
                final Node templatesRootNode = session.getNode(repositoryTarget);
                log.debug("Adding freemarker template: {}", templateName);
                final Node templateNode = templatesRootNode.addNode(templateName, "hst:template");
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

    private void defineTarget(final PluginContext context) {
        final String myTemplateName = (String) context.getPlaceholderData().get("templateName");
        if (!Strings.isNullOrEmpty(myTemplateName) && myTemplateName.equals("repository")) {
            repoBased = true;
        }
        if (repoBased) {
            // define repository target based on target path:
            repositoryTarget = getTarget();
            final CharSequence freemarkerRoot = (CharSequence) context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_SITE_FREEMARKER_ROOT);
            // replace freemarker root
            if (repositoryTarget.contains(freemarkerRoot)) {
                repositoryTarget = repositoryTarget.replace(freemarkerRoot, "");
                final Iterable<String> split = Splitter.on("/").omitEmptyStrings().trimResults().split(repositoryTarget);
                final List<String> parts = Lists.newArrayList(split);
                final int size = parts.size();
                if (size == 0) {
                    log.error("Cannot extract template name and configuration from target: {}", getTarget());
                }
                String configurationName;
                if (size > 1) {
                    configurationName = parts.get(0);
                    // check if hst default:
                    if (configurationName.equals("hstdefault") || configurationName.equals("include")) {
                        configurationName = "hst:default";
                    }
                } else {
                    configurationName = "hst:default";
                }
                templateName = parts.get(size - 1);
                this.templateName = EXTENSION_REPLACEMENT.matcher(templateName).replaceAll("");
                repositoryTarget = "/hst:hst/hst:configurations/"+ configurationName + "/hst:templates";

            }

        }
    }



    public String getTemplateName() {
        return templateName;
    }

    public String getRepositoryTarget() {
        return repositoryTarget;
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
        if (Strings.isNullOrEmpty(getTarget())) {
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


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FreemarkerInstruction{");
        sb.append("").append(super.toString());
        sb.append('}');
        return sb.toString();
    }
}
