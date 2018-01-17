/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.onehippo.cms7.essentials.plugin.sdk.rest.MavenDependency;
import org.onehippo.cms7.essentials.plugin.sdk.rest.MavenRepository;
import org.onehippo.cms7.essentials.plugin.sdk.service.model.Module;
import org.onehippo.cms7.essentials.plugin.sdk.service.MavenDependencyService;
import org.onehippo.cms7.essentials.plugin.sdk.service.MavenRepositoryService;
import org.onehippo.cms7.essentials.plugin.sdk.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.essentials.plugin.sdk.utils.ProjectUtils.ENT_GROUP_ID;
import static org.onehippo.cms7.essentials.plugin.sdk.utils.ProjectUtils.ENT_RELEASE_ID;
import static org.onehippo.cms7.essentials.plugin.sdk.utils.ProjectUtils.ENT_REPO_ID;
import static org.onehippo.cms7.essentials.plugin.sdk.utils.ProjectUtils.ENT_REPO_NAME;
import static org.onehippo.cms7.essentials.plugin.sdk.utils.ProjectUtils.ENT_REPO_URL;

/**
 * @version "$Id$"
 */
public final class EnterpriseUtils {

    private static final Logger LOG = LoggerFactory.getLogger(EnterpriseUtils.class);
    private static final MavenRepository ENTERPRISE_REPOSITORY = new MavenRepository();
    private static final MavenDependency EDITION_INDICATOR
            = new MavenDependency(ENT_GROUP_ID, "hippo-addon-edition-indicator");
    private static final MavenDependency APP_DEPENDENCIES_PACKAGE
            = new MavenDependency(ENT_GROUP_ID, "hippo-enterprise-package-app-dependencies", null, "pom", null);

    static {
        ENTERPRISE_REPOSITORY.setId(ENT_REPO_ID);
        ENTERPRISE_REPOSITORY.setName(ENT_REPO_NAME);
        ENTERPRISE_REPOSITORY.setUrl(ENT_REPO_URL);
        final MavenRepository.Policy releasePolicy = new MavenRepository.Policy();
        releasePolicy.setUpdatePolicy("never");
        releasePolicy.setChecksumPolicy("fail");
        ENTERPRISE_REPOSITORY.setReleasePolicy(releasePolicy);
    }

    private EnterpriseUtils() {
    }

    public static boolean upgradeToEnterpriseProject(final ProjectService projectService,
                                                     final MavenDependencyService dependencyService,
                                                     final MavenRepositoryService repositoryService) {
        final File pom = projectService.getPomPathForModule(Module.PROJECT).toFile();
        final Model pomModel = MavenModelUtils.readPom(pom);
        if (pomModel == null) {
            return false;
        }

        final Parent parent = pomModel.getParent();
        if (parent == null) {
            LOG.error("No parent element found in project root POM, cannot upgrade to Enterprise version.");
            return false;
        }

        if (!ENT_GROUP_ID.equals(parent.getGroupId()) || !ENT_RELEASE_ID.equals(parent.getArtifactId())) {
            parent.setArtifactId(ENT_RELEASE_ID);
            parent.setGroupId(ENT_GROUP_ID);
            if (!MavenModelUtils.writePom(pomModel, pom)) {
                return false;
            }
        }

        return repositoryService.addRepository(Module.PROJECT, ENTERPRISE_REPOSITORY)
                && dependencyService.addDependency(Module.CMS, EDITION_INDICATOR)
                && dependencyService.addDependency(Module.CMS, APP_DEPENDENCIES_PACKAGE);
    }
}
