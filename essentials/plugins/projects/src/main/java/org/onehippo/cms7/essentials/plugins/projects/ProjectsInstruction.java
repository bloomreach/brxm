/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.service.LoggingService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenAssemblyService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenCargoService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;

/**
 * Add WPM JDBC resource to context.xml.
 */
public class ProjectsInstruction implements Instruction {

    private static final String ENTERPRISE_SERVICES_ARTIFACTID = "hippo-enterprise-services";
    private static final String LOGGER_HST_BRANCH = "com.onehippo.cms.wpm.hst.configuration.branch";
    private static final String LOGGER_PROJECT = "com.onehippo.cms7.services.wpm.project";
    private static final String BPM_DISABLED_PROPERTY = "brx.wpm.external-bpm-engine.disabled";
    private static final MavenDependency DEPENDENCY_ENTERPRISE_SERVICES
            = new MavenDependency(ProjectService.GROUP_ID_ENTERPRISE, ENTERPRISE_SERVICES_ARTIFACTID);

    @Inject private LoggingService loggingService;
    @Inject private ProjectService projectService;
    @Inject private MavenCargoService mavenCargoService;
    @Inject private MavenAssemblyService mavenAssemblyService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        projectService.getLog4j2Files().forEach(f -> {
            loggingService.addLoggerToLog4jConfiguration(f, LOGGER_HST_BRANCH, "warn");
            loggingService.addLoggerToLog4jConfiguration(f, LOGGER_PROJECT, "warn");
        });

        // Install "enterprise services" JAR
        mavenCargoService.addDependencyToCargoSharedClasspath(DEPENDENCY_ENTERPRISE_SERVICES);
        mavenAssemblyService.addIncludeToFirstDependencySet("shared-lib-component.xml", DEPENDENCY_ENTERPRISE_SERVICES);

        // Set BPM disabled for local development
        mavenCargoService.addSystemProperty(BPM_DISABLED_PROPERTY, "true");

        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add Logger '" + LOGGER_HST_BRANCH + "' to log4j2 configuration files.");
        changeMessageQueue.accept(Type.EXECUTE, "Add Logger '" + LOGGER_PROJECT + "' to log4j2 configuration files.");
        changeMessageQueue.accept(Type.EXECUTE, "Add dependency '" + ProjectService.GROUP_ID_ENTERPRISE
                + ":" + ENTERPRISE_SERVICES_ARTIFACTID + "' to shared classpath of the Maven cargo plugin configuration.");
        changeMessageQueue.accept(Type.EXECUTE, "Add same dependency to distribution configuration file 'shared-lib-component.xml'.");
        changeMessageQueue.accept(Type.EXECUTE, "Add system property '" + BPM_DISABLED_PROPERTY
                + "' to the Maven cargo plugin configuration.");
    }
}
