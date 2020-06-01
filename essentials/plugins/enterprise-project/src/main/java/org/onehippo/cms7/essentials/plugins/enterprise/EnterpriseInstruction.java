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

import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenRepository;
import org.onehippo.cms7.essentials.sdk.api.service.MavenModelService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenRepositoryService;

public class EnterpriseInstruction implements Instruction {

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

    @Inject private MavenModelService modelService;
    @Inject private MavenRepositoryService repositoryService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        if (!modelService.setParentProject(Module.PROJECT, GROUP_ID_ENTERPRISE, ARTIFACT_ID_ENTERPRISE_RELEASE, null)) {
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
