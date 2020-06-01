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

package org.onehippo.cms7.essentials.plugin.crisp;

import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.service.MavenAssemblyService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenCargoService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;

public class CrispInstruction implements Instruction {

    private static final String CRISP_API_ARTIFACTID = "hippo-addon-crisp-api";
    private static final MavenDependency DEPENDENCY_CRISP_API
            = new MavenDependency(ProjectService.GROUP_ID_COMMUNITY, CRISP_API_ARTIFACTID);

    @Inject private MavenAssemblyService mavenAssemblyService;
    @Inject private MavenCargoService mavenCargoService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        mavenCargoService.addDependencyToCargoSharedClasspath(DEPENDENCY_CRISP_API);
        mavenAssemblyService.addIncludeToFirstDependencySet("shared-lib-component.xml", DEPENDENCY_CRISP_API);

        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add dependency '" + ProjectService.GROUP_ID_COMMUNITY
                + ":" + CRISP_API_ARTIFACTID + "' to shared classpath of the Maven cargo plugin configuration.");
        changeMessageQueue.accept(Type.EXECUTE, "Add same dependency to distribution configuration file 'shared-lib-component.xml'.");
    }
}
