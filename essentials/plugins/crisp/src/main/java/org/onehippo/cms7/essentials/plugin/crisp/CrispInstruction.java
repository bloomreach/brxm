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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrispInstruction implements Instruction {

    private static final Logger LOG = LoggerFactory.getLogger(CrispInstruction.class);

    @Inject private ProjectService projectService;

    @Override
    public Status execute(final Map<String, Object> parameters) {
        final Path hstConfigProperties = projectService.getWebInfPathForModule(Module.SITE_WEBAPP)
                .resolve("hst-config.properties");
        try {
            String oldContent = new String(Files.readAllBytes(hstConfigProperties));
            if (!oldContent.contains("crisp.moduleconfig.path")) {
                if (!oldContent.endsWith("\n")) {
                    oldContent += "\n";
                }
                oldContent += "\n";
                oldContent += "# Repository base path for this site application's CRISP configuration\n";
                oldContent += "# crisp.moduleconfig.path = /crisp/site/hippo:moduleconfig\n";
                Files.write(hstConfigProperties, oldContent.getBytes());
            }
        } catch (IOException e) {
            LOG.warn("Failed to update HST configuration properties:", e);
            return Status.FAILED;
        }

        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add HST configuration property to locate CRISP configuration in site webapp.");
    }
}
