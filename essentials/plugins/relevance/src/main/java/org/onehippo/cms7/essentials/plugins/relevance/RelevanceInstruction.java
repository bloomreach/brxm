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
import java.io.InputStream;

import com.google.common.collect.Multimap;

import org.apache.maven.model.Model;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.model.DependencyRestful;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.utils.ContextXMLUtils;
import org.onehippo.cms7.essentials.dashboard.utils.DependencyUtils;
import org.onehippo.cms7.essentials.dashboard.utils.Log4j2Utils;
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
    private static final String DEFAULT_WPM_RESOURCE = "<Resource name=\"jdbc/targetingDS\" auth=\"Container\" type=\"javax.sql.DataSource\"\n" +
            "          maxTotal=\"100\" maxIdle=\"10\" initialSize=\"10\" maxWaitMillis=\"10000\"\n" +
            "          testWhileIdle=\"true\" testOnBorrow=\"false\" validationQuery=\"SELECT 1\"\n" +
            "          timeBetweenEvictionRunsMillis=\"10000\" minEvictableIdleTimeMillis=\"60000\"\n" +
            "          username=\"sa\" password=\"\"\n" +
            "          driverClassName=\"org.h2.Driver\"\n" +
            "          url=\"jdbc:h2:${repo.path}/targeting/targeting;MVCC=TRUE\"/>";
    private static final String WPM_DATASOURCE_NAME = "jdbc/targetingDS";
    private static final String ENTERPRISE_GROUPID = "com.onehippo.cms7";

    @Override
    public InstructionStatus execute(final PluginContext context) {
        File contextXml = ProjectUtils.getContextXml();

        log.info("Adding jdbc/wpmDS datasource to conf/context.xml: {}", WPM_DATASOURCE_NAME);
        if (!ContextXMLUtils.hasResource(contextXml, WPM_DATASOURCE_NAME)) {
            ContextXMLUtils.addResource(contextXml, WPM_DATASOURCE_NAME, DEFAULT_WPM_RESOURCE);
        }
        ContextXMLUtils.addEnvironment(contextXml,
                "elasticsearch/targetingDS",
                "{'indexName':'visits', 'locations':['http://localhost:9200/']}",
                "java.lang.String", false);

        // Adding the dependencies in this Instruction instead of using plugin-descriptor.json
        // This so all configuration can be done in one phase
        log.info("Adding Relevance dependencies");
        addDependency(context, ENTERPRISE_GROUPID, "hippo-addon-targeting-dependencies-cms", null, "pom", "", "cms");
        addDependency(context, ENTERPRISE_GROUPID, "hippo-addon-targeting-dependencies-site", null, "pom", "", "site");
        addDependency(context, ENTERPRISE_GROUPID, "hippo-maxmind-geolite2", "20161123", "", "runtime", "site");

        log.info("Adding Relevance log4j2 logger");
        Log4j2Utils.addLoggerToLog4j2Files("com.onehippo.cms7.targeting", "warn");

        MavenCargoUtils.addPropertyToProfile(context, "elasticsearch.version","5.6.4", true);
        MavenCargoUtils.addPropertyToProfile(context, "maven.plugin.elasticsearch.version","5.7", true);

        InputStream pomStream = RelevanceInstruction.class.getResourceAsStream("/relevance-pom-overlay.xml");
        Model model = MavenModelUtils.readPom(pomStream);
        MavenCargoUtils.mergeCargoProfile(context, model);


        return InstructionStatus.SUCCESS;
    }

    private void addDependency(final PluginContext context, String groupId, String artifactId, String version, String type, String scope, String targetPom) {
        DependencyRestful dependency = new DependencyRestful();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setType(type);
        dependency.setScope(scope);
        dependency.setTargetPom(targetPom);
        DependencyUtils.addDependency(context, dependency);
    }

    public Multimap<MessageGroup, String> getChangeMessages() {
        return Instruction.makeChangeMessages(MessageGroup.EXECUTE, "Add H2 JDBC resource to context.xml.");
    }
}
