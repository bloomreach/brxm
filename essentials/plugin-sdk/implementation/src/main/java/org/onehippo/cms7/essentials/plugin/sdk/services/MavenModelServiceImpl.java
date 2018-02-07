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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.File;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.MavenModelService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MavenModelServiceImpl implements MavenModelService {

    private static final Logger LOG = LoggerFactory.getLogger(MavenModelServiceImpl.class);

    private final ProjectService projectService;

    @Inject
    public MavenModelServiceImpl(final ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public boolean setParentProject(final Module module, final String groupId, final String artifactId, final String version) {
        final File pom = projectService.getPomPathForModule(module).toFile();
        final Model pomModel = MavenModelUtils.readPom(pom);
        if (pomModel == null) {
            return false;
        }

        final Parent parent = pomModel.getParent();
        if (parent == null) {
            LOG.error("Failed to adjust parent model: no parent element found in POM.");
            return false;
        }

        parent.setGroupId(groupId);
        parent.setArtifactId(artifactId);
        if (version != null) {
            parent.setVersion(version);
        }
        return MavenModelUtils.writePom(pomModel, pom);
    }
}
