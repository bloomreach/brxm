/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.service.ContextXmlService;
import org.onehippo.cms7.essentials.sdk.api.service.LoggingService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenAssemblyService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenCargoService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;

/**
 * Add WPM JDBC resource to context.xml.
 */
public class ProjectsInstruction implements Instruction {

    private static final String WPM_RESOURCE_NAME = "jdbc/wpmDS";
    private static final Map<String, String> WPM_RESOURCE_ATTRIBUTES = new LinkedHashMap<>();
    private static final String WPM_WEBAPP_ARTIFACTID = "hippo-addon-wpm-camunda";
    private static final String ENTERPRISE_SERVICES_ARTIFACTID = "hippo-enterprise-services";
    private static final String WPM_WEBAPP_CONTEXT = "/bpm";
    private static final String LOGGER_HST_BRANCH = "com.onehippo.cms7.hst.configuration.branch";
    private static final String LOGGER_PROJECT = "com.onehippo.cms7.services.wpm.project";
    private static final MavenDependency DEPENDENCY_ENTERPRISE_SERVICES
            = new MavenDependency(ProjectService.GROUP_ID_ENTERPRISE, ENTERPRISE_SERVICES_ARTIFACTID);
    private static final MavenDependency DEPENDENCY_BPM_WAR
            = new MavenDependency(ProjectService.GROUP_ID_ENTERPRISE, WPM_WEBAPP_ARTIFACTID, null, "war", null);

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
    @Inject private MavenCargoService mavenCargoService;
    @Inject private MavenAssemblyService mavenAssemblyService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        contextXmlService.addResource(WPM_RESOURCE_NAME, WPM_RESOURCE_ATTRIBUTES);

        projectService.getLog4j2Files().forEach(f -> {
            loggingService.addLoggerToLog4jConfiguration(f, LOGGER_HST_BRANCH, "warn");
            loggingService.addLoggerToLog4jConfiguration(f, LOGGER_PROJECT, "warn");
        });

        // Install "enterprise services" JAR
        mavenCargoService.addDependencyToCargoSharedClasspath(DEPENDENCY_ENTERPRISE_SERVICES);
        mavenAssemblyService.addIncludeToFirstDependencySet("shared-lib-component.xml", DEPENDENCY_ENTERPRISE_SERVICES);

        // Install BPM WAR
        mavenCargoService.addDeployableToCargoRunner(DEPENDENCY_BPM_WAR, WPM_WEBAPP_CONTEXT);
        mavenAssemblyService.addDependencySet("webapps-component.xml", "webapps",
                "bpm.war", false, "provided", DEPENDENCY_BPM_WAR);

        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add Resource '" + WPM_RESOURCE_NAME + "' to Tomcat context.xml.");
        changeMessageQueue.accept(Type.EXECUTE, "Add Logger '" + LOGGER_HST_BRANCH + "' to log4j2 configuration files.");
        changeMessageQueue.accept(Type.EXECUTE, "Add Logger '" + LOGGER_PROJECT + "' to log4j2 configuration files.");
        changeMessageQueue.accept(Type.EXECUTE, "Add dependency '" + ProjectService.GROUP_ID_ENTERPRISE
                + ":" + ENTERPRISE_SERVICES_ARTIFACTID + "' to shared classpath of the Maven cargo plugin configuration.");
        changeMessageQueue.accept(Type.EXECUTE, "Add same dependency to distribution configuration file 'shared-lib-component.xml'.");
        changeMessageQueue.accept(Type.EXECUTE, "Add deployable '" + WPM_WEBAPP_ARTIFACTID
                + ".war' with context path '" + WPM_WEBAPP_CONTEXT + "' to deployables of Maven cargo plugin configuration.");
        changeMessageQueue.accept(Type.EXECUTE, "Add same web application to distribution configuration file 'webapps-component.xml'.");
    }
}
