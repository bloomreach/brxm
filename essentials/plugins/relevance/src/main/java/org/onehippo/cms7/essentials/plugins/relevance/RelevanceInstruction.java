/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.relevance;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.service.ContextXmlService;
import org.onehippo.cms7.essentials.dashboard.service.LoggingService;
import org.onehippo.cms7.essentials.dashboard.service.MavenCargoService;
import org.onehippo.cms7.essentials.dashboard.service.ProjectService;

/**
 * Add JDBC resource to context.xml.
 */
public class RelevanceInstruction implements Instruction {

    private static final String TARGETING_RESOURCE_NAME = "jdbc/targetingDS";
    private static final String TARGETING_ENVIRONMENT_NAME = "elasticsearch/targetingDS";
    private static final String TARGETING_LOGGER = "com.onehippo.cms7.targeting";
    private static final Map<String, String> TARGETING_RESOURCE_ATTRIBUTES = new LinkedHashMap<>();
    private static final Map<String, String> TARGETING_ENVIRONMENT_ATTRIBUTES = new LinkedHashMap<>();

    static {
        TARGETING_RESOURCE_ATTRIBUTES.put("auth", "Container");
        TARGETING_RESOURCE_ATTRIBUTES.put("type", "javax.sql.DataSource");
        TARGETING_RESOURCE_ATTRIBUTES.put("maxTotal", "100");
        TARGETING_RESOURCE_ATTRIBUTES.put("maxIdle", "10");
        TARGETING_RESOURCE_ATTRIBUTES.put("initialSize", "10");
        TARGETING_RESOURCE_ATTRIBUTES.put("maxWaitMillis", "10000");
        TARGETING_RESOURCE_ATTRIBUTES.put("testWhileIdle", "true");
        TARGETING_RESOURCE_ATTRIBUTES.put("testOnBorrow", "false");
        TARGETING_RESOURCE_ATTRIBUTES.put("validationQuery", "SELECT 1");
        TARGETING_RESOURCE_ATTRIBUTES.put("timeBetweenEvictionRunsMillis", "10000");
        TARGETING_RESOURCE_ATTRIBUTES.put("minEvictableIdleTimeMillis", "60000");
        TARGETING_RESOURCE_ATTRIBUTES.put("username", "sa");
        TARGETING_RESOURCE_ATTRIBUTES.put("password", "");
        TARGETING_RESOURCE_ATTRIBUTES.put("driverClassName", "org.h2.Driver");
        TARGETING_RESOURCE_ATTRIBUTES.put("url", "jdbc:h2:${repo.path}/targeting/targeting;MVCC=TRUE");

        TARGETING_ENVIRONMENT_ATTRIBUTES.put("value", "{'indexName':'visits', 'locations':['http://localhost:9200/']}");
        TARGETING_ENVIRONMENT_ATTRIBUTES.put("type", "java.lang.String");
    }

    @Inject private ContextXmlService contextXmlService;
    @Inject private LoggingService loggingService;
    @Inject private ProjectService projectService;
    @Inject private MavenCargoService mavenCargoService;

    @Override
    public InstructionStatus execute(final PluginContext context) {
        contextXmlService.addResource(TARGETING_RESOURCE_NAME, TARGETING_RESOURCE_ATTRIBUTES);
        contextXmlService.addEnvironment(TARGETING_ENVIRONMENT_NAME, TARGETING_ENVIRONMENT_ATTRIBUTES);

        projectService.getLog4j2Files()
                .forEach(f -> loggingService.addLoggerToLog4jConfiguration(f, TARGETING_LOGGER, "warn"));

        mavenCargoService.mergeCargoProfile(context, getClass().getResource("/relevance-pom-overlay.xml"));

        return InstructionStatus.SUCCESS;
    }

    public void populateChangeMessages(BiConsumer<MessageGroup, String> changeMessageQueue) {
        changeMessageQueue.accept(MessageGroup.EXECUTE, "Add Resource '" + TARGETING_RESOURCE_NAME + "' to context.xml.");
        changeMessageQueue.accept(MessageGroup.EXECUTE, "Add Environment '" + TARGETING_ENVIRONMENT_NAME + "' to context.xml.");
        changeMessageQueue.accept(MessageGroup.EXECUTE, "Add Logger '" + TARGETING_LOGGER + "' to log4j2 config files.");
        changeMessageQueue.accept(MessageGroup.EXECUTE, "Add Relevance-related configuration to Maven cargo plugin configuration.");
    }
}
