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

package org.onehippo.cms7.essentials.plugins.projects;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.apache.maven.model.Dependency;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.service.ContextXmlService;
import org.onehippo.cms7.essentials.dashboard.service.LoggingService;
import org.onehippo.cms7.essentials.dashboard.service.MavenAssemblyService;
import org.onehippo.cms7.essentials.dashboard.service.ProjectService;
import org.onehippo.cms7.essentials.dashboard.utils.MavenCargoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add WPM JDBC resource to context.xml.
 */
public class ProjectsInstruction implements Instruction {

    private static Logger log = LoggerFactory.getLogger(ProjectsInstruction.class);
    private static final String WPM_RESOURCE_NAME = "jdbc/wpmDS";
    private static final Map<String, String> WPM_RESOURCE_ATTRIBUTES = new LinkedHashMap<>();
    private static final String WPM_WEBAPP_ARTIFACTID = "hippo-addon-wpm-camunda";
    private static final String ENTERPRISE_SERVICES_ARTIFACTID = "hippo-enterprise-services";
    private static final String WPM_WEBAPP_CONTEXT = "/bpm";

    static {
        WPM_RESOURCE_ATTRIBUTES.put("auth", "Container");
        WPM_RESOURCE_ATTRIBUTES.put("type", "javax.sql.DataSource");
        WPM_RESOURCE_ATTRIBUTES.put("maxTotal", "100");
        WPM_RESOURCE_ATTRIBUTES.put("maxIdle", "10");
        WPM_RESOURCE_ATTRIBUTES.put("initialSize", "10");
        WPM_RESOURCE_ATTRIBUTES.put("maxWaitMillis", "10000");
        WPM_RESOURCE_ATTRIBUTES.put("testWhileIdle", "true");
        WPM_RESOURCE_ATTRIBUTES.put("testOnBorrow", "false");
        WPM_RESOURCE_ATTRIBUTES.put("validationQuery", "SELECT 1");
        WPM_RESOURCE_ATTRIBUTES.put("timeBetweenEvictionRunsMillis", "10000");
        WPM_RESOURCE_ATTRIBUTES.put("minEvictableIdleTimeMillis", "60000");
        WPM_RESOURCE_ATTRIBUTES.put("username", "sa");
        WPM_RESOURCE_ATTRIBUTES.put("password", "");
        WPM_RESOURCE_ATTRIBUTES.put("driverClassName", "org.h2.Driver");
        WPM_RESOURCE_ATTRIBUTES.put("url", "jdbc:h2:./wpm/wpm;AUTO_SERVER=TRUE");
    }

    @Inject private ContextXmlService contextXmlService;
    @Inject private LoggingService loggingService;
    @Inject private ProjectService projectService;
    @Inject private MavenAssemblyService mavenAssemblyService;

    @Override
    public InstructionStatus execute(final PluginContext context) {
        contextXmlService.addResource(WPM_RESOURCE_NAME, WPM_RESOURCE_ATTRIBUTES);

        projectService.getLog4j2Files().forEach(f -> {
            loggingService.addLoggerToLog4jConfiguration(f, "com.onehippo.cms7.hst.configuration.branch", "warn");
            loggingService.addLoggerToLog4jConfiguration(f, "com.onehippo.cms7.services.wpm.project", "warn");
        });

        log.info("Adding enterprise-services to the Cargo runner shared classpath");
        MavenCargoUtils.addDependencyToCargoSharedClasspath(context, ProjectService.GROUP_ID_ENTERPRISE, ENTERPRISE_SERVICES_ARTIFACTID);

        log.info("Adding BPM web application to the cargo.run profile with path: {}", WPM_WEBAPP_CONTEXT);
        final Dependency dependency = new Dependency();
        dependency.setGroupId(ProjectService.GROUP_ID_ENTERPRISE);
        dependency.setArtifactId(WPM_WEBAPP_ARTIFACTID);
        dependency.setType("war");

        MavenCargoUtils.addDeployableToCargoRunner(context, dependency, WPM_WEBAPP_CONTEXT);

        final String bpmWar = String.format("%s:%s:war", ProjectService.GROUP_ID_ENTERPRISE, WPM_WEBAPP_ARTIFACTID);
        mavenAssemblyService.addDependencySet("webapps-component.xml", "webapps",
                "bpm.war", false, "provided", bpmWar);
        final String servicesJar = String.format("%s:%s", ProjectService.GROUP_ID_ENTERPRISE, ENTERPRISE_SERVICES_ARTIFACTID);
        mavenAssemblyService.addIncludeToFirstDependencySet("shared-lib-component.xml", servicesJar);

        return InstructionStatus.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<MessageGroup, String> changeMessageQueue) {
        changeMessageQueue.accept(MessageGroup.EXECUTE,
                "Adjust project in several ways to install the 'Projects' feature.");
    }
}
