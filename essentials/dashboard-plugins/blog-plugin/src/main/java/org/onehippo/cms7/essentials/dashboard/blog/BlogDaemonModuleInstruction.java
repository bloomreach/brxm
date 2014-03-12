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

package org.onehippo.cms7.essentials.dashboard.blog;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures Daemon module for blog imports and derived data (author)
 *
 * @version "$Id$"
 */
public class BlogDaemonModuleInstruction implements Instruction {

    public static final String PREFIX = "importer_";
    private static final Pattern PREFIX_PATTERN = Pattern.compile(PREFIX);
    private static Logger log = LoggerFactory.getLogger(BlogDaemonModuleInstruction.class);

    private static final String CONFIG_DATA = "/blog-importer/blogscheduler/blogJobSchedule/scheduler:jobConfiguration";
    private static final String CONFIG_SCHEDULER = "/blog-importer/blogscheduler/blogJobSchedule";
    private static final String CONFIG_EVENT_BUS = "/hippo:configuration/hippo:modules/essentials-eventbus-listener/hippo:moduleconfig";


    @Override
    public String getMessage() {
        return "Configured Blog Daemon module";
    }

    @Override
    public void setMessage(final String message) {

    }

    @Override
    public String getAction() {
        return null;
    }

    @Override
    public void setAction(final String action) {

    }

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        final Map<String, Object> placeholderData = context.getPlaceholderData();
        final Session session = context.createSession();
        try {
            final Node eventBusNode = session.getNode(CONFIG_EVENT_BUS);
            eventBusNode.setProperty("projectNamespace", (String) placeholderData.get("namespace"));
            session.save();
            final String runSetup = (String) placeholderData.get(PREFIX + "setupImport");
            if (!Boolean.valueOf(runSetup)) {
                log.info("setupImport was disabled: {}", runSetup);
                return InstructionStatus.SKIPPED;
            }

            final Node schedulerNode = session.getNode(CONFIG_SCHEDULER);
            final Node dataNode = session.getNode(CONFIG_DATA);
            final Collection<String> urls = new HashSet<>();
            final Collection<String> authors = new HashSet<>();
            for (Map.Entry<String, Object> entry : placeholderData.entrySet()) {
                final String originalKey = entry.getKey();
                // skip invalid keys
                if (!originalKey.startsWith(PREFIX)) {
                    continue;
                }
                final String key = PREFIX_PATTERN.matcher(originalKey).replaceFirst("");
                final String value = (String) entry.getValue();
                final String schedulerPropName = MessageFormat.format("scheduler:{0}", key);
                if (key.equals("active") || key.equals("runInstantly")) {
                    schedulerNode.setProperty(schedulerPropName, Boolean.valueOf(value));
                } else if (key.equals("cronExpression") || key.equals("cronExpressionDescription") || key.equals("jobClassName")) {
                    schedulerNode.setProperty(schedulerPropName, value);
                } else if (key.startsWith("url")) {
                    urls.add(value);
                } else if (key.startsWith("authorsBasePath")) {
                    dataNode.setProperty(key, value);
                } else if (key.startsWith("author")) {
                    authors.add(value);
                } else {
                    dataNode.setProperty(key, value);
                }
            }
            final String[] myUrls = urls.toArray(new String[urls.size()]);
            dataNode.setProperty("urls", myUrls);
            final String[] myAuthors = authors.toArray(new String[authors.size()]);
            dataNode.setProperty("authors", myAuthors);

            session.save();
        } catch (RepositoryException e) {
            log.error("Error setting up Blog importer module", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        log.info("placeholderData {}", placeholderData);
        return InstructionStatus.SUCCESS;
    }

    @Override
    public void processPlaceholders(final Map<String, Object> data) {

    }
}
