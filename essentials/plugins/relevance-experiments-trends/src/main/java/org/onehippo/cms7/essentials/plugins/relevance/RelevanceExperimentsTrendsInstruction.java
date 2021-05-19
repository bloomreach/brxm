/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.service.ContextXmlService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenCargoService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add JDBC resource to context.xml.
 */
public class RelevanceExperimentsTrendsInstruction implements Instruction {

    private static Logger log = LoggerFactory.getLogger(RelevanceExperimentsTrendsInstruction.class);

    private static final String TARGETING_ENVIRONMENT_NAME = "elasticsearch/targetingDS";
    private static final Map<String, String> TARGETING_ENVIRONMENT_ATTRIBUTES = new LinkedHashMap<>();
    private static final String ELASTICSEARCH_DISABLED_PROPERTY = "targeting.elastic.disabled";

    static {
        TARGETING_ENVIRONMENT_ATTRIBUTES.put("value", "{'indexName':'visits', 'locations':['http://localhost:9200/']}");
        TARGETING_ENVIRONMENT_ATTRIBUTES.put("type", "java.lang.String");
    }

    @Inject private ContextXmlService contextXmlService;
    @Inject private MavenCargoService mavenCargoService;
    @Inject private ProjectService projectService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        contextXmlService.addEnvironment(TARGETING_ENVIRONMENT_NAME, TARGETING_ENVIRONMENT_ATTRIBUTES);

        mavenCargoService.mergeCargoProfile(getClass().getResource("/pom-overlay-elasticsearch.xml"));

        try {
            removePlatformProperty(ELASTICSEARCH_DISABLED_PROPERTY);
        } catch(IOException e) {
            log.error("Error removing property {}.", ELASTICSEARCH_DISABLED_PROPERTY, e);
            return Status.FAILED;
        }
        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add Environment '" + TARGETING_ENVIRONMENT_NAME + "' to context.xml.");
        changeMessageQueue.accept(Type.EXECUTE, "Add ElasticSearch related configuration to Maven cargo plugin configuration.");
        changeMessageQueue.accept(Type.EXECUTE, "Remove property " + ELASTICSEARCH_DISABLED_PROPERTY + " from " +
                projectService.getPlatformHstConfigPropertiesPath() + ".");
    }

    private void removePlatformProperty(String property) throws IOException
    {
        try (Stream<String> lines = Files.lines(projectService.getPlatformHstConfigPropertiesPath())) {
            List<String> out = lines
                    .filter(line -> !line.startsWith(property))
                    .collect(Collectors.toList());
            Files.write(projectService.getPlatformHstConfigPropertiesPath(), out, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        }
    }
}
