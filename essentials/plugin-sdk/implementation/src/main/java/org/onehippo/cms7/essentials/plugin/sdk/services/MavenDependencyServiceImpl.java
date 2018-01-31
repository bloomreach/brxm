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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.File;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.MavenDependencyService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MavenDependencyServiceImpl implements MavenDependencyService {

    private static final Logger LOG = LoggerFactory.getLogger(MavenDependencyServiceImpl.class);

    @Inject private ProjectService projectService;

    @Override
    public boolean hasDependency(final Module module, final MavenDependency dependency) {
        return updatePomModel(module, model -> hasDependency(model, dependency));
    }

    @Override
    public boolean addDependency(final Module module, final MavenDependency dependency) {
        return updatePomModel(module, model -> {
            if (hasDependency(model, dependency)) {
                return true;
            }
            model.addDependency(forMaven(dependency));
            return MavenModelUtils.writePom(model, projectService.getPomPathForModule(module).toFile());
        });
    }

    private Dependency forMaven(final MavenDependency dependency) {
        final Dependency dep = new Dependency();

        dep.setGroupId(dependency.getGroupId());
        dep.setArtifactId(dependency.getArtifactId());
        dep.setVersion(dependency.getVersion());
        dep.setType(dependency.getType());
        dep.setScope(dependency.getScope());

        return dep;
    }

    private boolean hasDependency(final Model pomModel, final MavenDependency dependency) {
        return pomModel.getDependencies()
                .stream()
                .anyMatch(dep -> dep.getArtifactId().equals(dependency.getArtifactId())
                        && dep.getGroupId().equals(dependency.getGroupId())
                        && isVersionMatch(dep, dependency));
    }

    private boolean isVersionMatch(final Dependency current, final MavenDependency incoming) {
        final String incomingVersion = incoming.getVersion();
        if (StringUtils.isBlank(incomingVersion)) {
            // no incoming version, don't worry.
            return true;
        }

        final String currentVersion = current.getVersion();
        if (StringUtils.isBlank(currentVersion)) {
            // managed version, keep it so.
            return true;
        }

        if (currentVersion.startsWith("${")) {
            // parameterized version
            if (!currentVersion.equals(incomingVersion)) {
                LOG.warn("Maven dependency '{}' already exists, checking for version '{}', consider matching.",
                        current, incomingVersion);
            }
            return true;
        }

        // consider already present if current version is same or newer
        return new DefaultArtifactVersion(currentVersion).compareTo(new DefaultArtifactVersion(incomingVersion)) >= 0;
    }

    private boolean updatePomModel(final Module module, final Predicate<Model> checker) {
        final File pom = projectService.getPomPathForModule(module).toFile();
        final Model model = MavenModelUtils.readPom(pom);
        return model != null && checker.test(model);
    }
}
