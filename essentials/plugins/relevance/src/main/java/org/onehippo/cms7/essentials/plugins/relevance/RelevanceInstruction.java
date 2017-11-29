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

import java.io.File;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.service.LoggingService;
import org.onehippo.cms7.essentials.dashboard.service.ProjectService;
import org.onehippo.cms7.essentials.dashboard.utils.ContextXMLUtils;
import org.onehippo.cms7.essentials.dashboard.utils.MavenCargoUtils;
import org.onehippo.cms7.essentials.dashboard.utils.MavenModelUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add JDBC resource to context.xml.
 */
public class RelevanceInstruction implements Instruction {

    private static Logger log = LoggerFactory.getLogger(RelevanceInstruction.class);
    private static final String TARGETING_RESOURCE_NAME = "jdbc/targetingDS";
    private static final String TARGETING_RESOURCE = "<Resource name=\"" + TARGETING_RESOURCE_NAME + "\" auth=\"Container\" type=\"javax.sql.DataSource\"\n" +
            "          maxTotal=\"100\" maxIdle=\"10\" initialSize=\"10\" maxWaitMillis=\"10000\"\n" +
            "          testWhileIdle=\"true\" testOnBorrow=\"false\" validationQuery=\"SELECT 1\"\n" +
            "          timeBetweenEvictionRunsMillis=\"10000\" minEvictableIdleTimeMillis=\"60000\"\n" +
            "          username=\"sa\" password=\"\"\n" +
            "          driverClassName=\"org.h2.Driver\"\n" +
            "          url=\"jdbc:h2:${repo.path}/targeting/targeting;MVCC=TRUE\"/>";

    @Inject private LoggingService loggingService;
    @Inject private ProjectService projectService;

    @Override
    public InstructionStatus execute(final PluginContext context) {
        File contextXml = ProjectUtils.getContextXml();

        log.info("Adding jdbc/wpmDS datasource to conf/context.xml: {}", TARGETING_RESOURCE_NAME);
        if (!ContextXMLUtils.hasResource(contextXml, TARGETING_RESOURCE_NAME)) {
            ContextXMLUtils.addResource(contextXml, TARGETING_RESOURCE_NAME, TARGETING_RESOURCE);
        }
        ContextXMLUtils.addEnvironment(contextXml,
                "elasticsearch/targetingDS",
                "{'indexName':'visits', 'locations':['http://localhost:9200/']}",
                "java.lang.String", false);

        projectService.getLog4j2Files()
                .forEach(f -> loggingService.addLoggerToLog4jConfiguration(f, "com.onehippo.cms7.targeting", "warn"));

        Model model = MavenModelUtils.readPom(getClass().getResourceAsStream("/relevance-pom-overlay.xml"));
        MavenCargoUtils.mergeCargoProfile(context, model);

        return InstructionStatus.SUCCESS;
    }

    public void populateChangeMessages(BiConsumer<MessageGroup, String> changeMessageQueue) {
        changeMessageQueue.accept(MessageGroup.EXECUTE, "Add H2 JDBC resource to context.xml.");
    }
}
