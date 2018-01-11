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

package org.onehippo.cms7.essentials.dashboard.blog;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures Daemon module for blog imports and derived data (author)
 *
 * @version "$Id$"
 */
public class BlogDaemonModuleInstruction implements Instruction {

    private static final Logger log = LoggerFactory.getLogger(BlogDaemonModuleInstruction.class);
    private static final String PREFIX = "importer_";
    private static final Pattern PREFIX_PATTERN = Pattern.compile(PREFIX);
    private static final String CONFIG_EVENT_BUS = "/hippo:configuration/hippo:modules/essentials-eventbus-listener/hippo:moduleconfig";

    @Inject private JcrService jcrService;

    @Override
    public InstructionStatus execute(final PluginContext context) {
        final Map<String, Object> placeholderData = context.getPlaceholderData();
        final Session session = jcrService.createSession();
        if (session != null) {
            try {
                final Node eventBusNode = session.getNode(CONFIG_EVENT_BUS);
                eventBusNode.setProperty("projectNamespace", (String) placeholderData.get("namespace"));
                session.save();
                final Boolean runSetup = (Boolean) placeholderData.remove(PREFIX + "setupImport");
                if (runSetup == null || !runSetup) {
                    log.info("setupImport was disabled");
                    return InstructionStatus.SKIPPED;
                }
                final Collection<String> urls = new HashSet<>();
                final Collection<String> authors = new HashSet<>();
                for (Map.Entry<String, Object> entry : placeholderData.entrySet()) {
                    final String originalKey = entry.getKey();
                    // skip invalid keys
                    if (!originalKey.startsWith(PREFIX)) {
                        continue;
                    }
                    final String key = PREFIX_PATTERN.matcher(originalKey).replaceFirst("");

                    if (key.startsWith("url")) {
                        urls.add((String) entry.getValue());
                    } else if (key.startsWith("author") && !"authorsBasePath".equals(key)) {
                        authors.add((String) entry.getValue());
                    } else if (key.equals("active") || key.equals("runInstantly")) {
                        eventBusNode.setProperty(key, (Boolean) entry.getValue());
                    } else {
                        eventBusNode.setProperty(key, (String) entry.getValue());
                    }
                }
                final String[] myUrls = urls.toArray(new String[urls.size()]);
                eventBusNode.setProperty("urls", myUrls);
                final String[] myAuthors = authors.toArray(new String[authors.size()]);
                eventBusNode.setProperty("authors", myAuthors);

                session.save();
            } catch (RepositoryException e) {
                log.error("Error setting up Blog importer module", e);
            } finally {
                jcrService.destroySession(session);
            }
        }
        log.info("placeholderData {}", placeholderData);
        return InstructionStatus.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<MessageGroup, String> changeMessageQueue) {
        changeMessageQueue.accept(MessageGroup.EXECUTE, "Configure Blog Daemon module");
    }
}
