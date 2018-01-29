/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.enterprise;

import java.io.File;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenRepository;
import org.onehippo.cms7.essentials.sdk.api.service.MavenRepositoryService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnterpriseInstruction implements Instruction {

    private static final Logger LOG = LoggerFactory.getLogger(EnterpriseInstruction.class);
    private static final String GROUP_ID_ENTERPRISE = "com.onehippo.cms7";
    private static final String ARTIFACT_ID_ENTERPRISE_RELEASE = "hippo-cms7-enterprise-release";
    private static final MavenRepository ENTERPRISE_REPOSITORY = new MavenRepository();

    static {
        ENTERPRISE_REPOSITORY.setId("hippo-maven2-enterprise");
        ENTERPRISE_REPOSITORY.setName("Hippo Enterprise Maven 2");
        ENTERPRISE_REPOSITORY.setUrl("https://maven.onehippo.com/maven2-enterprise");
        final MavenRepository.Policy releasePolicy = new MavenRepository.Policy();
        releasePolicy.setUpdatePolicy("never");
        releasePolicy.setChecksumPolicy("fail");
        ENTERPRISE_REPOSITORY.setReleasePolicy(releasePolicy);
    }

    @Inject private ProjectService projectService;
    @Inject private MavenRepositoryService repositoryService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        final File pom = projectService.getPomPathForModule(Module.PROJECT).toFile();
        final Model pomModel = MavenModelUtils.readPom(pom);
        if (pomModel == null) {
            return Status.FAILED;
        }

        final Parent parent = pomModel.getParent();
        if (parent == null) {
            LOG.error("No parent element found in project root POM, cannot upgrade to Enterprise version.");
            return Status.FAILED;
        }

        parent.setGroupId(GROUP_ID_ENTERPRISE);
        parent.setArtifactId(ARTIFACT_ID_ENTERPRISE_RELEASE);
        if (!MavenModelUtils.writePom(pomModel, pom)) {
            return Status.FAILED;
        }

        if (!repositoryService.addRepository(Module.PROJECT, ENTERPRISE_REPOSITORY)) {
            return Status.FAILED;
        }

        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Change the parent of the project root POM to refer to the Enterprise Release.");
        changeMessageQueue.accept(Type.EXECUTE, "Add the Enterprise repository to the project root POM.");
    }
}
